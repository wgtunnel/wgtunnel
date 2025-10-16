package com.zaneschepke.wireguardautotunnel.viewmodel

import androidx.lifecycle.ViewModel
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.domain.repository.GlobalEffectRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.domain.sideeffect.GlobalSideEffect
import com.zaneschepke.wireguardautotunnel.ui.state.TunnelUiState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

@HiltViewModel(assistedFactory = TunnelViewModel.Factory::class)
class TunnelViewModel
@AssistedInject
constructor(
    private val tunnelRepository: TunnelRepository,
    private val globalEffectRepository: GlobalEffectRepository,
    private val tunnelManager: TunnelManager,
    @Assisted val tunnelId: Int,
) : ContainerHost<TunnelUiState, Nothing>, ViewModel() {

    override val container =
        container<TunnelUiState, Nothing>(
            TunnelUiState(),
            buildSettings = { repeatOnSubscribedStopTimeout = 5000L },
        ) {
            combine(
                    tunnelRepository.userTunnelsFlow.map {
                        it.firstOrNull { tun -> tun.id == tunnelId }
                    },
                    tunnelManager.activeTunnels.map { it.containsKey(tunnelId) },
                ) { tunnel, active ->
                    state.copy(tunnel = tunnel, isActive = active, isLoading = tunnel == null)
                }
                .collect { reduce { it } }
        }

    fun setTunnelPingIp(ip: String) = intent {
        val tunnel = state.tunnel ?: return@intent
        tunnelRepository.save(tunnel.copy(pingTarget = ip.ifBlank { null }))
    }

    fun setRestartOnPing(to: Boolean) = intent {
        val tunnel = state.tunnel ?: return@intent
        tunnelRepository.save(tunnel.copy(restartOnPingFailure = to))
    }

    fun togglePrimaryTunnel() = intent {
        val tunnel = state.tunnel ?: return@intent
        val update = if (tunnel.isPrimaryTunnel) null else tunnel
        tunnelRepository.updatePrimaryTunnel(update)
    }

    fun setMobileDataTunnel(to: Boolean) = intent {
        val tunnel = state.tunnel ?: return@intent
        tunnelRepository.save(tunnel.copy(isMobileDataTunnel = to))
    }

    fun setEthernetTunnel(to: Boolean) = intent {
        val tunnel = state.tunnel ?: return@intent
        tunnelRepository.save(tunnel.copy(isEthernetTunnel = to))
    }

    fun toggleIpv4Preferred() = intent {
        val latestTunnel = state.tunnel ?: return@intent
        tunnelRepository.save(latestTunnel.copy(isIpv4Preferred = !latestTunnel.isIpv4Preferred))
    }

    private suspend fun postSideEffect(globalSideEffect: GlobalSideEffect) {
        globalEffectRepository.post(globalSideEffect)
    }

    @AssistedFactory
    interface Factory {
        fun create(tunnelId: Int): TunnelViewModel
    }
}
