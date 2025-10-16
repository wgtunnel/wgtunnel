package com.zaneschepke.wireguardautotunnel.viewmodel

import androidx.lifecycle.ViewModel
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.domain.repository.AppStateRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.GeneralSettingRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.GlobalEffectRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.domain.sideeffect.GlobalSideEffect
import com.zaneschepke.wireguardautotunnel.ui.sideeffect.LocalSideEffect
import com.zaneschepke.wireguardautotunnel.ui.state.SharedAppUiState
import com.zaneschepke.wireguardautotunnel.ui.theme.Theme
import com.zaneschepke.wireguardautotunnel.util.LocaleUtil
import com.zaneschepke.wireguardautotunnel.util.RootShellUtils
import com.zaneschepke.wireguardautotunnel.util.StringValue
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import xyz.teamgravity.pin_lock_compose.PinManager

@HiltViewModel
class SharedAppViewModel
@Inject
constructor(
    private val appStateRepository: AppStateRepository,
    private val serviceManager: ServiceManager,
    private val tunnelManager: TunnelManager,
    private val globalEffectRepository: GlobalEffectRepository,
    private val tunnelRepository: TunnelRepository,
    private val settingsRepository: GeneralSettingRepository,
    private val rootShellUtils: RootShellUtils,
) : ContainerHost<SharedAppUiState, LocalSideEffect>, ViewModel() {

    val globalSideEffect = globalEffectRepository.flow

    override val container =
        container<SharedAppUiState, LocalSideEffect>(
            SharedAppUiState(),
            buildSettings = { repeatOnSubscribedStopTimeout = 5000L },
        ) {
            intent {
                combine(
                        appStateRepository.flow,
                        tunnelRepository.userTunnelsFlow.map { tuns ->
                            tuns.associate { it.id to it.name }
                        },
                        serviceManager.autoTunnelService.map { it != null },
                        settingsRepository.flow,
                    ) { appState, tunnelNames, autoTunnelActive, settings ->
                        state.copy(
                            theme = settings.theme,
                            locale = settings.locale ?: LocaleUtil.OPTION_PHONE_LANGUAGE,
                            pinLockEnabled = settings.isPinLockEnabled,
                            isAutoTunnelActive = autoTunnelActive,
                            tunnelNames = tunnelNames,
                            settings = settings,
                            isLocationDisclosureShown = appState.isLocationDisclosureShown,
                            isBatteryOptimizationShown = appState.isBatteryOptimizationDisableShown,
                            isAppLoaded = true,
                        )
                    }
                    .collect { newState -> reduce { newState } }
            }

            intent {
                tunnelManager.errorEvents.collect { (tunnel, message) ->
                    postSideEffect(GlobalSideEffect.Snackbar(message.toStringValue()))
                }
            }

            intent {
                tunnelManager.messageEvents.collect { (_, message) ->
                    postSideEffect(GlobalSideEffect.Snackbar(message.toStringValue()))
                }
            }
        }

    fun startTunnel(tunnelConfig: TunnelConfig) = intent {
        if (state.settings.appMode == AppMode.VPN) {
            if (!serviceManager.hasVpnPermission())
                return@intent postSideEffect(
                    GlobalSideEffect.RequestVpnPermission(AppMode.VPN, tunnelConfig)
                )
        }
        tunnelManager.startTunnel(tunnelConfig)
    }

    fun postSideEffect(localSideEffect: LocalSideEffect) = intent {
        postSideEffect(localSideEffect)
    }

    fun setLocationDisclosureShown() = intent {
        appStateRepository.setLocationDisclosureShown(true)
    }

    fun setTheme(theme: Theme) = intent {
        settingsRepository.upsert(state.settings.copy(theme = theme))
    }

    fun setLocale(locale: String) = intent {
        settingsRepository.upsert(state.settings.copy(locale = locale))
        postSideEffect(GlobalSideEffect.ConfigChanged)
    }

    fun setPinLockEnabled(enabled: Boolean) = intent {
        if (!enabled) PinManager.clearPin()
        settingsRepository.upsert(state.settings.copy(isPinLockEnabled = enabled))
    }

    fun setSelectedTunnelCount(count: Int) = intent {
        reduce { state.copy(selectedTunnelCount = count) }
    }

    fun stopTunnel(tunnelConfig: TunnelConfig) = intent {
        tunnelManager.stopTunnel(tunnelConfig.id)
    }

    fun setAppMode(appMode: AppMode) = intent {
        when (appMode) {
            AppMode.VPN,
            AppMode.PROXY -> Unit
            AppMode.LOCK_DOWN -> {
                if (!serviceManager.hasVpnPermission()) {
                    return@intent postSideEffect(
                        GlobalSideEffect.RequestVpnPermission(appMode, null)
                    )
                }
            }
            AppMode.KERNEL -> {
                val accepted = rootShellUtils.requestRoot()
                val message =
                    if (!accepted) StringValue.StringResource(R.string.error_root_denied)
                    else StringValue.StringResource(R.string.root_accepted)
                postSideEffect(GlobalSideEffect.Snackbar(message))
                if (!accepted) return@intent
            }
        }
        settingsRepository.upsert(state.settings.copy(appMode = appMode))
    }

    suspend fun postSideEffect(globalSideEffect: GlobalSideEffect) {
        globalEffectRepository.post(globalSideEffect)
    }

    fun authenticated() = intent { reduce { state.copy(isPinVerified = true) } }

    suspend fun postGlobalSideEffect(sideEffect: GlobalSideEffect) {
        globalEffectRepository.post(sideEffect)
    }

    fun showSnackMessage(message: StringValue) = intent {
        postGlobalSideEffect(GlobalSideEffect.Snackbar(message))
    }

    fun showToast(message: StringValue) = intent { postSideEffect(GlobalSideEffect.Toast(message)) }

    fun disableBatteryOptimizationsShown() = intent {
        appStateRepository.setBatteryOptimizationDisableShown(true)
    }
}
