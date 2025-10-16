package com.zaneschepke.wireguardautotunnel.viewmodel

import androidx.lifecycle.ViewModel
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.domain.repository.MonitoringSettingsRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.ui.state.MonitoringUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

@HiltViewModel
class MonitoringViewModel
@Inject
constructor(
    private val monitoringSettingsRepository: MonitoringSettingsRepository,
    private val tunnelRepository: TunnelRepository,
    private val tunnelsRepository: TunnelRepository,
) : ContainerHost<MonitoringUiState, Nothing>, ViewModel() {

    override val container =
        container<MonitoringUiState, Nothing>(
            MonitoringUiState(),
            buildSettings = { repeatOnSubscribedStopTimeout = 5000L },
        ) {
            combine(monitoringSettingsRepository.flow, tunnelRepository.flow) {
                    monitoringSettings,
                    tunnels ->
                    state.copy(
                        monitoringSettings = monitoringSettings,
                        tunnels = tunnels,
                        isLoading = false,
                    )
                }
                .collect { reduce { it } }
        }

    fun setPingEnabled(to: Boolean) = intent {
        monitoringSettingsRepository.upsert(state.monitoringSettings.copy(isPingEnabled = to))
    }

    fun setTunnelPingIntervalSeconds(to: Int) = intent {
        monitoringSettingsRepository.upsert(
            state.monitoringSettings.copy(tunnelPingIntervalSeconds = to)
        )
    }

    fun setTunnelPingAttempts(to: Int) = intent {
        monitoringSettingsRepository.upsert(state.monitoringSettings.copy(tunnelPingAttempts = to))
    }

    fun setTunnelPingTimeoutSeconds(to: Int?) = intent {
        monitoringSettingsRepository.upsert(
            state.monitoringSettings.copy(tunnelPingTimeoutSeconds = to)
        )
    }

    fun setDetailedPingStats(to: Boolean) = intent {
        monitoringSettingsRepository.upsert(
            state.monitoringSettings.copy(showDetailedPingStats = to)
        )
    }

    fun setLocalLogging(to: Boolean) = intent {
        monitoringSettingsRepository.upsert(state.monitoringSettings.copy(isLocalLogsEnabled = to))
    }

    fun setPingTarget(tunnel: TunnelConfig, target: String?) = intent {
        tunnelsRepository.save(tunnel.copy(pingTarget = target?.ifBlank { null }))
    }
}
