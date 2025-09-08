package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.zaneschepke.logcatter.LogReader
import com.zaneschepke.networkmonitor.NetworkMonitor
import com.zaneschepke.wireguardautotunnel.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.domain.state.FailureReason
import com.zaneschepke.wireguardautotunnel.domain.state.PingState
import com.zaneschepke.wireguardautotunnel.util.extensions.toMillis
import com.zaneschepke.wireguardautotunnel.util.network.NetworkUtils
import dagger.hilt.android.scopes.ServiceScoped
import io.ktor.util.collections.*
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.amnezia.awg.crypto.Key
import timber.log.Timber

@ServiceScoped
class TunnelMonitor
@Inject
constructor(
    private val appDataRepository: AppDataRepository,
    private val tunnelManager: TunnelManager,
    private val networkMonitor: NetworkMonitor,
    private val networkUtils: NetworkUtils,
    private val logReader: LogReader,
) {

    @OptIn(FlowPreview::class)
    suspend fun startMonitoring(tunnelId: Int, withLogs: Boolean): Job = coroutineScope {
        launch {
            val config = appDataRepository.tunnels.getById(tunnelId) ?: return@launch
            launch { startPingMonitor(config) }
            launch { startWgStatsPoll(config.id) }
            if (withLogs) launch { startLogsMonitor(config) }
        }
    }

    private suspend fun startLogsMonitor(tunnelConf: TunnelConf) {
        logReader.liveLogs.collect { log ->
            if (!log.tag.contains(tunnelConf.tunName)) return@collect
            val healthLogs =
                when {
                    log.message.contains(HANDSHAKE_RESPONSE_TEXT, true) ||
                        log.message.contains(KEEPALIVE_RESPONSE_TEXT, true) -> true
                    log.message.contains(HANDSHAKE_INIT_FAILED_TEXT, true) ||
                        log.message.contains(HANDSHAKE_NOT_COMPLETED_TEXT) ||
                        log.message.contains(DATA_PACKET_FAILED_TEXT) -> false

                    else -> null
                }
            healthLogs?.let { healthy ->
                tunnelManager.updateTunnelStatus(tunnelConf.id, null, null, null, healthy)
            }
        }
    }

    private suspend fun startPingMonitor(tunnelConf: TunnelConf) = coroutineScope {
        val pingStatsFlow = MutableStateFlow<Map<Key, PingState>>(emptyMap())

        val tunStateFlow =
            tunnelManager.activeTunnels.mapNotNull { it[tunnelConf.id] }.stateIn(this)

        val connectivityStateFlow = networkMonitor.connectivityStateFlow.stateIn(this)

        val isNetworkConnected = connectivityStateFlow.map { it.hasConnectivity() }.stateIn(this)

        data class NetworkChangeKey(
            val ethernetConnected: Boolean,
            val wifiConnected: Boolean,
            val cellularConnected: Boolean,
            val wifiSsid: String?,
        )

        connectivityStateFlow
            .map {
                NetworkChangeKey(
                    ethernetConnected = it.ethernetConnected,
                    wifiConnected = it.wifiState.connected,
                    cellularConnected = it.cellularConnected,
                    wifiSsid = if (it.wifiState.connected) it.wifiState.ssid else null,
                )
            }
            .distinctUntilChanged()
            .stateIn(this)

        appDataRepository.settings.flow
            .distinctUntilChanged { old, new ->
                old.isPingEnabled == new.isPingEnabled &&
                    old.tunnelPingIntervalSeconds == new.tunnelPingIntervalSeconds &&
                    old.tunnelPingAttempts == new.tunnelPingAttempts &&
                    old.tunnelPingTimeoutSeconds == new.tunnelPingTimeoutSeconds
                old.appMode == new.appMode
            }
            .collectLatest { settings ->
                if (!settings.isPingEnabled) return@collectLatest
                // TODO for now until we get monitoring for these modes
                if (settings.appMode == AppMode.LOCK_DOWN || settings.appMode == AppMode.PROXY)
                    return@collectLatest

                Timber.d("Starting pinger for ${tunnelConf.tunName} with settings")

                val config = tunnelConf.toAmConfig()

                val pingablePeers = config.peers.filter { it.allowedIps.isNotEmpty() }
                if (pingablePeers.isEmpty()) return@collectLatest

                suspend fun performPing() {
                    val updates = ConcurrentMap<Key, PingState>()

                    pingablePeers.forEach { peer ->
                        val previousState = pingStatsFlow.value[peer.publicKey] ?: PingState()

                        val allowedIpStr = peer.allowedIps.firstOrNull()?.toString()
                        if (allowedIpStr == null) {
                            updates[peer.publicKey] =
                                previousState.copy(
                                    isReachable = false,
                                    failureReason = FailureReason.NoResolvedEndpoint,
                                    lastPingAttemptMillis = System.currentTimeMillis(),
                                )
                            return@forEach
                        }

                        val host =
                            tunnelConf.pingTarget
                                ?: {
                                        val parts = allowedIpStr.split("/")
                                        val internalIp =
                                            if (parts.size == 2) parts[0] else allowedIpStr

                                        val prefix =
                                            if (parts.size == 2) parts[1].toIntOrNull() ?: 32
                                            else 32
                                        if (prefix <= 1) {
                                            CLOUDFLARE_IPV4_IP
                                        } else {
                                            internalIp.removeSurrounding("[", "]")
                                        }
                                    }
                                    .invoke()

                        val attemptTime = System.currentTimeMillis()
                        runCatching {
                                val pingStats =
                                    settings.tunnelPingTimeoutSeconds?.let {
                                        networkUtils.pingWithStats(
                                            host,
                                            settings.tunnelPingAttempts,
                                            it.toMillis(),
                                        )
                                    }
                                        ?: networkUtils.pingWithStats(
                                            host,
                                            settings.tunnelPingAttempts,
                                        )

                                updates[peer.publicKey] =
                                    previousState.copy(
                                        transmitted = pingStats.transmitted,
                                        received = pingStats.received,
                                        packetLoss = pingStats.packetLoss,
                                        rttMin = pingStats.rttMin,
                                        rttMax = pingStats.rttMax,
                                        rttAvg = pingStats.rttAvg,
                                        rttStddev = pingStats.rttStddev,
                                        isReachable = pingStats.isReachable,
                                        failureReason =
                                            if (pingStats.isReachable) null
                                            else FailureReason.PingFailed,
                                        lastSuccessfulPingMillis =
                                            pingStats.lastSuccessfulPingMillis
                                                ?: previousState.lastSuccessfulPingMillis,
                                        pingTarget = host,
                                        lastPingAttemptMillis = attemptTime,
                                    )
                                Timber.d(
                                    "Ping completed for peer ${peer.publicKey.toBase64().substring(0, 5)}.. to host $host with stats: $pingStats"
                                )
                            }
                            .onFailure {
                                Timber.e(
                                    it,
                                    "Ping failed for peer ${peer.publicKey} in ${tunnelConf.tunName} to host $host",
                                )
                                updates[peer.publicKey] =
                                    previousState.copy(
                                        isReachable = false,
                                        failureReason = FailureReason.PingFailed,
                                        pingTarget = host,
                                        lastPingAttemptMillis = attemptTime,
                                    )
                            }
                    }

                    if (updates.isNotEmpty()) {
                        pingStatsFlow.update { updates }
                        tunnelManager.updateTunnelStatus(tunnelConf.id, null, null, updates)
                    }
                }

                // Wait for the tunnel to be fully active
                tunStateFlow.filter { state -> state.status == TunnelStatus.Up }.first()

                // small delay to make sure tunnel is fully up before we actively monitor
                delay(3_000L)

                while (isActive) {
                    if (isNetworkConnected.value) {
                        performPing()
                    } else {
                        pingStatsFlow.update { current ->
                            current.mapValues { entry ->
                                entry.value.copy(
                                    isReachable = false,
                                    failureReason = FailureReason.NoConnectivity,
                                    lastPingAttemptMillis = System.currentTimeMillis(),
                                )
                            }
                        }
                        tunnelManager.updateTunnelStatus(
                            tunnelConf.id,
                            null,
                            null,
                            pingStatsFlow.value,
                        )
                    }
                    delay(settings.tunnelPingIntervalSeconds.toMillis())
                }
            }
    }

    private suspend fun startWgStatsPoll(tunnelId: Int) = coroutineScope {
        while (isActive) {
            val stats = tunnelManager.getStatistics(tunnelId)
            tunnelManager.updateTunnelStatus(tunnelId, null, stats, null)
            delay(STATS_DELAY)
        }
    }

    companion object {
        const val CLOUDFLARE_IPV6_IP = "2606:4700:4700::1111"
        const val CLOUDFLARE_IPV4_IP = "1.1.1.1"

        const val STATS_DELAY = 1_000L

        const val KEEPALIVE_RESPONSE_TEXT = "Receiving keepalive packet"
        const val HANDSHAKE_RESPONSE_TEXT = "Received handshake response"
        const val HANDSHAKE_INIT_FAILED_TEXT = "Failed to send handshake initiation: write udp"
        const val DATA_PACKET_FAILED_TEXT = "Failed to send data packets"
        const val HANDSHAKE_NOT_COMPLETED_TEXT =
            "Handshake did not complete after 5 seconds, retrying"
    }
}
