package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.zaneschepke.wireguardautotunnel.domain.enums.BackendState
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.events.BackendError
import com.zaneschepke.wireguardautotunnel.domain.events.BackendMessage
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
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class TunnelManager
@Inject
constructor(
    private val kernelTunnel: TunnelProvider,
    private val userspaceTunnel: TunnelProvider,
    private val appDataRepository: AppDataRepository,
    applicationScope: CoroutineScope,
    ioDispatcher: CoroutineDispatcher,
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

    override val activeTunnels: StateFlow<Map<TunnelConf, TunnelState>> =
        tunnelProviderFlow.value.activeTunnels

    @OptIn(ExperimentalCoroutinesApi::class)
    override val errorEvents: SharedFlow<Pair<TunnelConf, BackendError>> =
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
            .filterNotNull()
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

    override fun setBackendState(backendState: BackendState, allowedIps: Collection<String>) {
        tunnelProviderFlow.value.setBackendState(backendState, allowedIps)
    }

    override fun getBackendState(): BackendState {
        return tunnelProviderFlow.value.getBackendState()
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

    suspend fun restorePreviousState() {
        val settings = appDataRepository.settings.get()
        if (settings.isRestoreOnBootEnabled) {
            val previouslyActiveTuns = appDataRepository.tunnels.getActive()
            val tunsToStart =
                previouslyActiveTuns.filterNot { tun ->
                    activeTunnels.value.any { tun.id == it.key.id }
                }
            if (settings.isKernelEnabled) {
                return tunsToStart.forEach { startTunnel(it) }
            } else {
                tunsToStart.firstOrNull()?.let { startTunnel(it) }
            }
        }
    }
}
