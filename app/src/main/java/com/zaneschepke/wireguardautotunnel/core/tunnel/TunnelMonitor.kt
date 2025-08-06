package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.zaneschepke.logcatter.LogReader
import com.zaneschepke.networkmonitor.NetworkMonitor
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.domain.state.FailureReason
import com.zaneschepke.wireguardautotunnel.domain.state.PingState
import com.zaneschepke.wireguardautotunnel.util.extensions.toMillis
import com.zaneschepke.wireguardautotunnel.util.network.NetworkUtils
import dagger.hilt.android.scopes.ServiceScoped
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.amnezia.awg.crypto.Key
import timber.log.Timber
import javax.inject.Inject

@ServiceScoped
class TunnelMonitor @Inject constructor(
    private val appDataRepository: AppDataRepository,
    private val tunnelManager: TunnelManager,
    private val networkMonitor: NetworkMonitor,
    private val networkUtils: NetworkUtils,
    private val logReader: LogReader,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    private val tunnelJobs = mutableMapOf<TunnelConf, Job>()

    private val pingStatsFlows = mutableMapOf<TunnelConf, MutableStateFlow<Map<Key, PingState>>>()

    fun startMonitoring(callerScope: CoroutineScope, tunnelConf: TunnelConf) {
        if (tunnelJobs.containsKey(tunnelConf)) return
        pingStatsFlows[tunnelConf] = MutableStateFlow(emptyMap())
        tunnelJobs[tunnelConf] = callerScope.launch {
            launch { startTunnelConfChangesJob(tunnelConf) }
            launch { startPingMonitor(tunnelConf) }
            launch { startWgStatsPoll(tunnelConf) }  // Add WG stats polling
            launch { startLogsMonitor() }  // Global, could be started once if needed
        }
    }

    fun stopMonitoring(tunnelConf: TunnelConf) {
        tunnelJobs.remove(tunnelConf)?.cancel()
        pingStatsFlows.remove(tunnelConf)
    }

    private suspend fun startTunnelConfChangesJob(tunnelConf: TunnelConf) {
        appDataRepository.tunnels.flow
            .map { storedTunnels -> storedTunnels.firstOrNull { it.id == tunnelConf.id } }
            .filterNotNull()
            .distinctUntilChanged { old, new -> old == new }
            .collect { storedTunnel ->
                if (tunnelConf != storedTunnel) {
                    Timber.d("Config changed for ${storedTunnel.tunName}, bouncing")
                    withContext(NonCancellable) {
                        tunnelManager.bounceTunnel(
                            storedTunnel,
                            TunnelStatus.StopReason.ConfigChanged,
                        )
                    }
                }
            }
    }

    private suspend fun startLogsMonitor() {
        logReader.liveLogs.collect { message ->
            // TODO monitor logs for handshake failures for additional monitoring
        }
    }

    private suspend fun startPingMonitor(tunnelConf: TunnelConf) = coroutineScope {
        val tunStateFlow = tunnelManager.activeTunnels.mapNotNull { it.getValueById(tunnelConf.id) }.stateIn(this)

        val connectivityStateFlow = networkMonitor.connectivityStateFlow.stateIn(this)

        val isNetworkConnected = connectivityStateFlow.map { it.hasConnectivity() }.stateIn(this)

        data class NetworkChangeKey(
            val ethernetConnected: Boolean,
            val wifiConnected: Boolean,
            val cellularConnected: Boolean,
            val wifiSsid: String?
        )

        val networkChangeFlow = connectivityStateFlow.map {
            NetworkChangeKey(
                ethernetConnected = it.ethernetConnected,
                wifiConnected = it.wifiState.connected,
                cellularConnected = it.cellularConnected,
                wifiSsid = if (it.wifiState.connected) it.wifiState.ssid else null
            )
        }.distinctUntilChanged().stateIn(this)

        appDataRepository.settings.flow.distinctUntilChanged { old, new ->
            old.isPingEnabled == new.isPingEnabled &&
                    old.tunnelPingIntervalSeconds == new.tunnelPingIntervalSeconds &&
                    old.tunnelPingAttempts == new.tunnelPingAttempts &&
                    old.tunnelPingTimeoutSeconds == new.tunnelPingTimeoutSeconds
        }.collectLatest { settings ->

            if(!settings.isPingEnabled) return@collectLatest

            Timber.d("Starting pinger for ${tunnelConf.tunName} with settings")

            val config = tunnelConf.toAmConfig()

            val pingablePeers = config.peers.filter { it.allowedIps.isNotEmpty() }
            if (pingablePeers.isEmpty()) return@collectLatest

            val pingStatsFlow = pingStatsFlows[tunnelConf] ?: return@collectLatest

            // Initialize
            pingStatsFlow.value = emptyMap()

            suspend fun performPing() {
                val tunState = tunStateFlow.value

                val updates = mutableMapOf<Key, PingState>()

                pingablePeers.forEach { peer ->
                    val previousState = pingStatsFlow.value[peer.publicKey] ?: PingState()

                    val allowedIpStr = peer.allowedIps.firstOrNull()?.toString()
                    if (allowedIpStr == null) {
                        updates[peer.publicKey] = previousState.copy(isReachable = false, failureReason = FailureReason.NoResolvedEndpoint, consecutiveFailures = 0)
                        return@forEach
                    }

                    val host = {
                        val parts = allowedIpStr.split("/")
                        val internalIp = if (parts.size == 2) parts[0] else allowedIpStr

                        val prefix = if (parts.size == 2) parts[1].toIntOrNull() ?: 32 else 32
                        if (prefix <= 1) {
                            tunnelConf.pingTarget ?: {
                                val resolvedEndpoint = tunState.statistics?.peerStats(peer.publicKey)?.resolvedEndpoint
                                val host = resolvedEndpoint?.replace(Regex(":\\d+$"), "")?.removeSurrounding("[", "]")
                                if (host?.contains(":") == true) CLOUDFLARE_IPV6_IP else CLOUDFLARE_IPV4_IP
                            }.invoke()
                        } else {
                            internalIp.removeSurrounding("[", "]")
                        }
                    }.invoke()

                    runCatching {
                        val pingStats = settings.tunnelPingTimeoutSeconds?.let {
                            networkUtils.pingWithStats(host, settings.tunnelPingAttempts, it.toMillis())
                        } ?: networkUtils.pingWithStats(host, settings.tunnelPingAttempts)

                        updates[peer.publicKey] = previousState.copy(
                            transmitted = pingStats.transmitted,
                            received = pingStats.received,
                            packetLoss = pingStats.packetLoss,
                            rttMin = pingStats.rttMin,
                            rttMax = pingStats.rttMax,
                            rttAvg = pingStats.rttAvg,
                            rttStddev = pingStats.rttStddev,
                            isReachable = pingStats.isReachable,
                            failureReason = if (pingStats.isReachable) null else FailureReason.PingFailed,
                            lastSuccessfulPingMillis = pingStats.lastSuccessfulPingMillis ?: previousState.lastSuccessfulPingMillis,
                            pingTarget = host,
                            consecutiveFailures = 0
                        )
                        Timber.d(
                            "Ping successful for peer ${peer.publicKey.toBase64().substring(0, 5)}.. to host $host with stats: $pingStats"
                        )
                    }.onFailure {
                        Timber.e(it, "Ping failed for peer ${peer.publicKey} in ${tunnelConf.tunName} to host $host")
                        updates[peer.publicKey] = previousState.copy(
                            isReachable = false,
                            failureReason = FailureReason.PingFailed,
                            pingTarget = host,
                            consecutiveFailures = previousState.consecutiveFailures + 1
                        )
                    }
                }

                if (updates.isNotEmpty()) {
                    pingStatsFlow.update { current ->
                        val newMap = current.toMutableMap()
                        newMap.putAll(updates)
                        newMap
                    }
                    tunnelManager.updateTunnelStatus(tunnelConf, null, null, pingStatsFlow.value)
                }
            }

            // Wait for the tunnel to be fully active
            tunStateFlow.filter { state ->
                state.status == TunnelStatus.Up
            }.first()

            // Handle initial network state upon starting/restarting the collector
            val initialConnected = isNetworkConnected.value
            if (!initialConnected) {
                Timber.d("Initial no network connectivity for ${tunnelConf.tunName}")
                pingStatsFlow.update { current ->
                    current.mapValues { entry -> entry.value.copy(isReachable = false, failureReason = FailureReason.NoConnectivity, consecutiveFailures = 0) }
                }
                tunnelManager.updateTunnelStatus(tunnelConf, null, null, pingStatsFlow.value)
            } else {
                performPing()
            }

            // Launch a separate coroutine to monitor network changes
            launch {
                // Drop the first emission since initial state is already handled above
                networkChangeFlow.drop(1).collect { _ ->
                    val connected = isNetworkConnected.value
                    if (connected) {
                        Timber.d("Network change detected for ${tunnelConf.tunName}, triggering immediate ping")
                        performPing()
                    } else {
                        Timber.d("Network connectivity lost for ${tunnelConf.tunName}")
                        pingStatsFlow.update { current ->
                            current.mapValues { entry -> entry.value.copy(isReachable = false, failureReason = FailureReason.NoConnectivity, consecutiveFailures = 0) }
                        }
                        tunnelManager.updateTunnelStatus(tunnelConf, null, null, pingStatsFlow.value)
                    }
                }
            }

            // Main loop for scheduled pings, running independently of network changes
            while (isActive) {
                delay(settings.tunnelPingIntervalSeconds.toMillis())
                if (isNetworkConnected.value) {
                    performPing()
                } else {
                    pingStatsFlow.update { current ->
                        current.mapValues { entry -> entry.value.copy(isReachable = false, failureReason = FailureReason.NoConnectivity, consecutiveFailures = 0) }
                    }
                    tunnelManager.updateTunnelStatus(tunnelConf, null, null, pingStatsFlow.value)
                }
            }
        }
    }

    private suspend fun startWgStatsPoll(tunnelConf: TunnelConf) = coroutineScope {
        while (isActive) {
            val stats = tunnelManager.getStatistics(tunnelConf)
            tunnelManager.updateTunnelStatus(tunnelConf, null, stats, null)
            delay(STATS_DELAY)
        }
    }

    companion object {

        const val STATS_DELAY = 1_000L
        const val CLOUDFLARE_IPV6_IP = "2606:4700:4700::1111"
        const val CLOUDFLARE_IPV4_IP = "1.1.1.1"

        // ipv6 disabled or block on network
        // Failed to send handshake initiation: write udp [::]
        // Failed to send data packets: write udp [::]
        // Failed to send data packets: write udp 0.0.0.0:51820
        // Handshake did not complete after 5 seconds, retrying
    }
}