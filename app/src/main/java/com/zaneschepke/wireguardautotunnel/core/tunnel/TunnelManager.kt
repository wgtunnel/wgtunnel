package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.di.*
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendMode
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.events.BackendCoreException
import com.zaneschepke.wireguardautotunnel.domain.events.BackendMessage
import com.zaneschepke.wireguardautotunnel.domain.model.GeneralSettings
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.domain.state.PingState
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelStatistics
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.plus
import org.amnezia.awg.crypto.Key
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalCoroutinesApi::class)
class TunnelManager
@Inject
constructor(
    @Kernel private val kernelTunnel: TunnelProvider,
    @Userspace private val userspaceTunnel: TunnelProvider,
    @ProxyUserspace private val proxyUserspaceTunnel: TunnelProvider,
    private val serviceManager: ServiceManager,
    private val appDataRepository: AppDataRepository,
    @ApplicationScope applicationScope: CoroutineScope,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : TunnelProvider {

    @OptIn(ExperimentalAtomicApi::class)
    private val tunnelProviderFlow: StateFlow<TunnelProvider> = run {
        val currentBackend = AtomicReference(userspaceTunnel)
        val currentSettings = AtomicReference(GeneralSettings())
        val initialEmit = AtomicBoolean(true)

        appDataRepository.settings.flow
            .filterNotNull()
            // ignore default state
            .filterNot { it == GeneralSettings() }
            .distinctUntilChanged { old, new ->
                old.appMode == new.appMode &&
                    old.isLanOnKillSwitchEnabled == new.isLanOnKillSwitchEnabled
            }
            .map { settings ->
                Timber.d("App mode changes with ${settings.appMode}")
                val backend =
                    when (settings.appMode) {
                        AppMode.VPN -> userspaceTunnel
                        AppMode.PROXY -> proxyUserspaceTunnel
                        AppMode.LOCK_DOWN -> proxyUserspaceTunnel
                        AppMode.KERNEL -> kernelTunnel
                    }
                settings to backend
            }
            .onEach { (settings, newBackend) ->
                val isInitialEmit = initialEmit.exchange(false)
                val oldBackend = currentBackend.exchange(newBackend)
                val oldSettings = currentSettings.exchange(settings)

                if ((oldSettings.appMode != settings.appMode) && !isInitialEmit) {
                    oldBackend.stopTunnel()
                    if (oldSettings.appMode == AppMode.LOCK_DOWN)
                        proxyUserspaceTunnel.setBackendMode(BackendMode.Inactive)
                }
                if (settings.appMode == AppMode.LOCK_DOWN) {
                    // kill switch will always catch all ipv6, just add ipv4 networks for allowsIps
                    val allowedIps =
                        if (settings.isLanOnKillSwitchEnabled) TunnelConf.IPV4_PUBLIC_NETWORKS
                        else emptySet()
                    try {
                        // TODO handle situation where they don't have vpn permission, request it
                        if (hasVpnPermission()) {
                            proxyUserspaceTunnel.setBackendMode(BackendMode.KillSwitch(allowedIps))
                        }
                    } catch (e: BackendCoreException) {
                        // TODO expose this error to user
                        Timber.e(e)
                    }
                }
                // restore state if configured
                if (isInitialEmit && settings.isRestoreOnBootEnabled) {
                    Timber.d("Restoring previous state")
                    if (
                        settings.isAutoTunnelEnabled &&
                            serviceManager.autoTunnelService.value == null
                    ) {
                        serviceManager.startAutoTunnel()
                    } else {
                        val previouslyActiveTuns = appDataRepository.tunnels.getActive()
                        val tunsToStart =
                            previouslyActiveTuns.filterNot { tun ->
                                activeTunnels.value.any { tun.id == it.key.id }
                            }
                        tunsToStart.forEach { startTunnel(it) }
                    }
                }
            }
            .map { (_, backend) -> backend }
            .stateIn(
                scope = applicationScope.plus(ioDispatcher),
                started = SharingStarted.Eagerly,
                initialValue = userspaceTunnel,
            )
    }

    override val activeTunnels: StateFlow<Map<TunnelConf, TunnelState>> =
        tunnelProviderFlow
            .flatMapLatest { it.activeTunnels }
            .stateIn(
                scope = applicationScope.plus(ioDispatcher),
                started = SharingStarted.Eagerly,
                initialValue = emptyMap(),
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    override val errorEvents: SharedFlow<Pair<TunnelConf, BackendCoreException>> =
        tunnelProviderFlow
            .flatMapLatest { it.errorEvents }
            .shareIn(
                scope = applicationScope.plus(ioDispatcher),
                started = SharingStarted.Eagerly,
                replay = 0,
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    override val messageEvents: SharedFlow<Pair<TunnelConf, BackendMessage>> =
        tunnelProviderFlow
            .flatMapLatest { it.messageEvents }
            .shareIn(
                scope = applicationScope.plus(ioDispatcher),
                started = SharingStarted.Eagerly,
                replay = 0,
            )

    override val bouncingTunnelIds: ConcurrentHashMap<Int, TunnelStatus.StopReason> =
        tunnelProviderFlow.value.bouncingTunnelIds

    override fun hasVpnPermission(): Boolean {
        return userspaceTunnel.hasVpnPermission()
    }

    override fun getStatistics(tunnelConf: TunnelConf): TunnelStatistics? {
        return tunnelProviderFlow.value.getStatistics(tunnelConf)
    }

    override suspend fun startTunnel(tunnelConf: TunnelConf) {
        tunnelProviderFlow.value.startTunnel(tunnelConf)
    }

    override suspend fun stopTunnel(tunnelConf: TunnelConf?, reason: TunnelStatus.StopReason) {
        tunnelProviderFlow.value.stopTunnel(tunnelConf, reason)
    }

    override suspend fun bounceTunnel(tunnelConf: TunnelConf, reason: TunnelStatus.StopReason) {
        tunnelProviderFlow.value.bounceTunnel(tunnelConf, reason)
    }

    override fun setBackendMode(backendMode: BackendMode) {
        tunnelProviderFlow.value.setBackendMode(backendMode)
    }

    override fun getBackendMode(): BackendMode {
        return tunnelProviderFlow.value.getBackendMode()
    }

    override suspend fun runningTunnelNames(): Set<String> {
        return tunnelProviderFlow.value.runningTunnelNames()
    }

    override suspend fun updateTunnelStatus(
        tunnelConf: TunnelConf,
        status: TunnelStatus?,
        stats: TunnelStatistics?,
        pingStates: Map<Key, PingState>?,
        handshakeSuccessLogs: Boolean?,
    ) {
        tunnelProviderFlow.value.updateTunnelStatus(
            tunnelConf,
            status,
            stats,
            pingStates,
            handshakeSuccessLogs,
        )
    }
}
