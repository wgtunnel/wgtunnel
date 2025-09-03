package com.zaneschepke.wireguardautotunnel.viewmodel

import androidx.lifecycle.ViewModel
import com.zaneschepke.networkmonitor.NetworkMonitor
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.domain.repository.AppStateRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.GeneralSettingRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.GlobalEffectRepository
import com.zaneschepke.wireguardautotunnel.domain.sideeffect.GlobalSideEffect
import com.zaneschepke.wireguardautotunnel.ui.state.AutoTunnelUiState
import com.zaneschepke.wireguardautotunnel.util.StringValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class AutoTunnelViewModel
@Inject
constructor(
    private val settingsRepository: GeneralSettingRepository,
    private val serviceManager: ServiceManager,
    private val networkMonitor: NetworkMonitor,
    private val globalEffectRepository: GlobalEffectRepository,
    private val appStateRepository: AppStateRepository,
) : ContainerHost<AutoTunnelUiState, Nothing>, ViewModel() {

    override val container =
        container<AutoTunnelUiState, Nothing>(
            AutoTunnelUiState(),
            buildSettings = { repeatOnSubscribedStopTimeout = 5000L },
        ) {
            intent {
                combine(
                        networkMonitor.connectivityStateFlow,
                        serviceManager.autoTunnelService.map { it != null },
                        settingsRepository.flow,
                        appStateRepository.flow,
                    ) { connectivity, active, settings, appState ->
                        AutoTunnelUiState(
                            autoTunnelActive = active,
                            connectivityState = connectivity,
                            generalSettings = settings,
                            isBatteryOptimizationShown = appState.isBatteryOptimizationDisableShown,
                            stateInitialized = true,
                        )
                    }
                    .collect { reduce { it } }
            }
        }

    suspend fun postSideEffect(globalSideEffect: GlobalSideEffect) {
        globalEffectRepository.post(globalSideEffect)
    }

    fun setLocationDisclosureShown() = intent {
        appStateRepository.setLocationDisclosureShown(true)
    }

    fun toggleAutoTunnel() = intent {
        if (!state.autoTunnelActive) {
            when (state.generalSettings.appMode) {
                AppMode.VPN ->
                    if (!serviceManager.hasVpnPermission())
                        return@intent postSideEffect(
                            GlobalSideEffect.RequestVpnPermission(AppMode.VPN, null)
                        )
                else -> Unit
            }
            if (!state.isBatteryOptimizationShown) {
                return@intent postSideEffect(GlobalSideEffect.RequestBatteryOptimizationDisabled)
            }
            serviceManager.startAutoTunnel()
        } else {
            serviceManager.stopAutoTunnel()
        }
    }

    fun setAutoTunnelOnWifiEnabled(to: Boolean) = intent {
        settingsRepository.save(state.generalSettings.copy(isTunnelOnWifiEnabled = to))
    }

    fun setWildcardsEnabled(to: Boolean) = intent {
        settingsRepository.save(state.generalSettings.copy(isWildcardsEnabled = to))
    }

    fun setStopOnNoInternetEnabled(to: Boolean) = intent {
        settingsRepository.save(state.generalSettings.copy(isStopOnNoInternetEnabled = to))
    }

    fun saveTrustedNetworkName(name: String) = intent {
        if (name.isEmpty()) return@intent
        val trimmed = name.trim()
        if (state.generalSettings.trustedNetworkSSIDs.contains(name)) {
            return@intent postSideEffect(
                GlobalSideEffect.Snackbar(StringValue.StringResource(R.string.error_ssid_exists))
            )
        }
        setTrustedNetworkNames((state.generalSettings.trustedNetworkSSIDs + trimmed).toMutableSet())
    }

    fun setTrustedNetworkNames(to: Set<String>) = intent {
        settingsRepository.save(state.generalSettings.copy(trustedNetworkSSIDs = to))
    }

    fun removeTrustedNetworkName(name: String) = intent {
        setTrustedNetworkNames((state.generalSettings.trustedNetworkSSIDs - name).toMutableSet())
    }

    fun setTunnelOnCellular(to: Boolean) = intent {
        settingsRepository.save(state.generalSettings.copy(isTunnelOnMobileDataEnabled = to))
    }

    fun setTunnelOnEthernet(to: Boolean) = intent {
        settingsRepository.save(state.generalSettings.copy(isTunnelOnMobileDataEnabled = to))
    }

    fun setDebounceDelay(to: Int) = intent {
        settingsRepository.save(state.generalSettings.copy(debounceDelaySeconds = to))
    }
}
