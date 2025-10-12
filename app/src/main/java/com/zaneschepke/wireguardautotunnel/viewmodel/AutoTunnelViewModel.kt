package com.zaneschepke.wireguardautotunnel.viewmodel

import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.lifecycle.ViewModel
import com.zaneschepke.networkmonitor.NetworkMonitor
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.data.model.WifiDetectionMethod
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.AppStateRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.GeneralSettingRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.GlobalEffectRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.domain.sideeffect.GlobalSideEffect
import com.zaneschepke.wireguardautotunnel.ui.state.AutoTunnelUiState
import com.zaneschepke.wireguardautotunnel.util.RootShellUtils
import com.zaneschepke.wireguardautotunnel.util.StringValue
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import rikka.shizuku.Shizuku

@HiltViewModel
class AutoTunnelViewModel
@Inject
constructor(
    private val settingsRepository: GeneralSettingRepository,
    private val serviceManager: ServiceManager,
    private val networkMonitor: NetworkMonitor,
    private val globalEffectRepository: GlobalEffectRepository,
    private val tunnelsRepository: TunnelRepository,
    private val appStateRepository: AppStateRepository,
    private val rootShellUtils: RootShellUtils,
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
                        tunnelsRepository.userTunnelsFlow,
                    ) { connectivity, active, settings, appState, tunnels ->
                        state.copy(
                            autoTunnelActive = active,
                            connectivityState = connectivity,
                            settings = settings,
                            tunnels = tunnels,
                            isBatteryOptimizationShown = appState.isBatteryOptimizationDisableShown,
                            isLocationDisclosureShown = appState.isLocationDisclosureShown,
                            isLoading = false,
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
            when (state.settings.appMode) {
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
            settingsRepository.updateAutoTunnelEnabled(true)
        } else {
            settingsRepository.updateAutoTunnelEnabled(false)
        }
    }

    fun setAutoTunnelOnWifiEnabled(to: Boolean) = intent {
        settingsRepository.save(state.settings.copy(isTunnelOnWifiEnabled = to))
    }

    fun setWildcardsEnabled(to: Boolean) = intent {
        settingsRepository.save(state.settings.copy(isWildcardsEnabled = to))
    }

    fun setStopOnNoInternetEnabled(to: Boolean) = intent {
        settingsRepository.save(state.settings.copy(isStopOnNoInternetEnabled = to))
    }

    fun saveTrustedNetworkName(name: String) = intent {
        if (name.isEmpty()) return@intent
        val trimmed = name.trim()
        if (state.settings.trustedNetworkSSIDs.contains(name)) {
            return@intent postSideEffect(
                GlobalSideEffect.Snackbar(StringValue.StringResource(R.string.error_ssid_exists))
            )
        }
        setTrustedNetworkNames((state.settings.trustedNetworkSSIDs + trimmed).toMutableSet())
    }

    fun setTrustedNetworkNames(to: Set<String>) = intent {
        settingsRepository.save(state.settings.copy(trustedNetworkSSIDs = to))
    }

    fun removeTrustedNetworkName(name: String) = intent {
        setTrustedNetworkNames((state.settings.trustedNetworkSSIDs - name).toMutableSet())
    }

    fun setTunnelOnCellular(to: Boolean) = intent {
        settingsRepository.save(state.settings.copy(isTunnelOnMobileDataEnabled = to))
    }

    fun setTunnelOnEthernet(to: Boolean) = intent {
        settingsRepository.save(state.settings.copy(isTunnelOnEthernetEnabled = to))
    }

    fun setDebounceDelay(to: Int) = intent {
        settingsRepository.save(state.settings.copy(debounceDelaySeconds = to))
    }

    fun setPreferredMobileDataTunnel(tunnel: TunnelConf?) = intent {
        tunnelsRepository.updateMobileDataTunnel(tunnel)
    }

    fun setPreferredEthernetTunnel(tunnel: TunnelConf?) = intent {
        tunnelsRepository.updateEthernetTunnel(tunnel)
    }

    fun addTunnelNetwork(tunnel: TunnelConf, ssid: String) = intent {
        tunnelsRepository.save(
            tunnel.copy(tunnelNetworks = tunnel.tunnelNetworks.toMutableSet().apply { add(ssid) })
        )
    }

    fun removeTunnelNetwork(tunnel: TunnelConf, ssid: String) = intent {
        tunnelsRepository.save(
            tunnel.copy(
                tunnelNetworks = tunnel.tunnelNetworks.toMutableSet().apply { remove(ssid) }
            )
        )
    }

    fun setWifiDetectionMethod(method: WifiDetectionMethod) = intent {
        when (method) {
            WifiDetectionMethod.ROOT -> {
                val accepted = rootShellUtils.requestRoot()
                val message =
                    if (!accepted) StringValue.StringResource(R.string.error_root_denied)
                    else StringValue.StringResource(R.string.root_accepted)
                postSideEffect(GlobalSideEffect.Snackbar(message))
                if (!accepted) return@intent
            }
            WifiDetectionMethod.SHIZUKU -> {
                requestShizuku()
                return@intent
            }
            else -> Unit
        }
        settingsRepository.save(state.settings.copy(wifiDetectionMethod = method))
    }

    private fun requestShizuku() = intent {
        Shizuku.addRequestPermissionResultListener(
            Shizuku.OnRequestPermissionResultListener { requestCode: Int, grantResult: Int ->
                if (grantResult != PERMISSION_GRANTED) return@OnRequestPermissionResultListener
                intent {
                    settingsRepository.save(
                        state.settings.copy(wifiDetectionMethod = WifiDetectionMethod.SHIZUKU)
                    )
                }
            }
        )
        try {
            if (Shizuku.checkSelfPermission() != PERMISSION_GRANTED) Shizuku.requestPermission(123)
            settingsRepository.save(
                state.settings.copy(wifiDetectionMethod = WifiDetectionMethod.SHIZUKU)
            )
        } catch (_: Exception) {
            postSideEffect(
                GlobalSideEffect.Snackbar(StringValue.StringResource(R.string.shizuku_not_detected))
            )
        }
    }
}
