package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.core.notification.NotificationManager
import com.zaneschepke.wireguardautotunnel.core.notification.WireGuardNotification
import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.di.Kernel
import com.zaneschepke.wireguardautotunnel.di.Userspace
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendError
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendState
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelStatistics
import com.zaneschepke.wireguardautotunnel.util.StringValue
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class TunnelManager
@Inject
constructor(
    @Kernel private val kernelTunnel: TunnelProvider,
    @Userspace private val userspaceTunnel: TunnelProvider,
    private val appDataRepository: AppDataRepository,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val notificationManager: NotificationManager,
) : TunnelProvider {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val tunnelProviderFlow =
        appDataRepository.settings.flow
            .filterNotNull()
            .flatMapLatest { settings ->
                MutableStateFlow(if (settings.isKernelEnabled) kernelTunnel else userspaceTunnel)
            }
            .stateIn(
                scope = applicationScope.plus(ioDispatcher),
                started = SharingStarted.Eagerly,
                initialValue = userspaceTunnel,
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    override val activeTunnels =
        appDataRepository.settings.flow
            .filterNotNull()
            .flatMapLatest { settings ->
                if (settings.isKernelEnabled) {
                    kernelTunnel.activeTunnels
                } else {
                    userspaceTunnel.activeTunnels
                }
            }
            .stateIn(
                scope = applicationScope.plus(ioDispatcher),
                started = SharingStarted.Eagerly,
                initialValue = emptyMap(),
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    override val errorEvents: SharedFlow<Pair<TunnelConf, BackendError>> =
        combine(
                tunnelProviderFlow.flatMapLatest { it.errorEvents },
                WireGuardAutoTunnel.uiActive,
            ) { errorEvent, isEnabled ->
                if (isEnabled) errorEvent else null
            }
            .filterNotNull()
            .shareIn(
                scope = applicationScope.plus(ioDispatcher),
                started = SharingStarted.WhileSubscribed(5_000),
                replay = 0,
            )

    // observe tunnel errors and launch notifications if ui is inactive
    init {
        applicationScope.launch(ioDispatcher) {
            tunnelProviderFlow
                .flatMapLatest { it.errorEvents }
                .collect { (tunnelConf, error) ->
                    if (!WireGuardAutoTunnel.uiActive.value) {
                        val notification =
                            notificationManager.createNotification(
                                WireGuardNotification.NotificationChannels.VPN,
                                title = StringValue.DynamicString(tunnelConf.name),
                                description =
                                    StringValue.StringResource(
                                        R.string.tunnel_error_template,
                                        error.toStringRes(),
                                    ),
                            )
                        notificationManager.show(
                            NotificationManager.TUNNEL_STATUS_NOTIFICATION_ID,
                            notification,
                        )
                    }
                }
        }
    }

    override val bouncingTunnelIds: ConcurrentHashMap<Int, TunnelStatus.StopReason> =
        tunnelProviderFlow.value.bouncingTunnelIds

    override fun hasVpnPermission(): Boolean {
        return userspaceTunnel.hasVpnPermission()
    }

    override suspend fun updateTunnelStatistics(tunnel: TunnelConf) {
        tunnelProviderFlow.value.updateTunnelStatistics(tunnel)
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

    override fun setBackendState(backendState: BackendState, allowedIps: Collection<String>) {
        tunnelProviderFlow.value.setBackendState(backendState, allowedIps)
    }

    override fun getBackendState(): BackendState {
        return tunnelProviderFlow.value.getBackendState()
    }

    override suspend fun runningTunnelNames(): Set<String> {
        return tunnelProviderFlow.value.runningTunnelNames()
    }

    override fun getStatistics(tunnelConf: TunnelConf): TunnelStatistics? {
        return tunnelProviderFlow.value.getStatistics(tunnelConf)
    }

    fun restorePreviousState(): Job =
        applicationScope.launch(ioDispatcher) {
            val settings = appDataRepository.settings.get()
            if (settings.isRestoreOnBootEnabled) {
                val previouslyActiveTuns = appDataRepository.tunnels.getActive()
                val tunsToStart =
                    previouslyActiveTuns.filterNot { tun ->
                        activeTunnels.value.any { tun.id == it.key.id }
                    }
                if (settings.isKernelEnabled) {
                    return@launch tunsToStart.forEach { startTunnel(it) }
                } else {
                    tunsToStart.firstOrNull()?.let { startTunnel(it) }
                }
            }
        }
}
