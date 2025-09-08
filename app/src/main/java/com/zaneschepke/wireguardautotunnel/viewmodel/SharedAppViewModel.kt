package com.zaneschepke.wireguardautotunnel.viewmodel

import androidx.compose.runtime.Composable
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.lifecycle.ViewModel
import com.wireguard.android.util.RootShell
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.data.model.WifiDetectionMethod
import com.zaneschepke.wireguardautotunnel.di.AppShell
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.AppStateRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.GeneralSettingRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.GlobalEffectRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.domain.sideeffect.GlobalSideEffect
import com.zaneschepke.wireguardautotunnel.ui.sideeffect.LocalSideEffect
import com.zaneschepke.wireguardautotunnel.ui.state.SharedAppUiState
import com.zaneschepke.wireguardautotunnel.ui.theme.Theme
import com.zaneschepke.wireguardautotunnel.util.LocaleUtil
import com.zaneschepke.wireguardautotunnel.util.StringValue
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import rikka.shizuku.Shizuku
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
    @AppShell private val rootShell: Provider<RootShell>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
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
                        serviceManager.autoTunnelService.map { it != null },
                        settingsRepository.flow,
                        tunnelRepository.flow,
                    ) { appState, autoTunnelActive, settings, tunnels ->
                        state.copy(
                            theme = appState.theme,
                            locale = appState.locale ?: LocaleUtil.OPTION_PHONE_LANGUAGE,
                            pinLockEnabled = appState.isPinLockEnabled,
                            isAutoTunnelActive = autoTunnelActive,
                            tunnels = tunnels,
                            settings = settings,
                            isLocationDisclosureShown = appState.isLocationDisclosureShown,
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
                // TODO could improve this, maybe merge with errors, improve messages
                tunnelManager.messageEvents.collect { (_, message) ->
                    postSideEffect(GlobalSideEffect.Snackbar(message.toStringValue()))
                }
            }
        }

    fun startTunnel(tunnelConf: TunnelConf) = intent {
        if (state.settings.appMode == AppMode.VPN) {
            if (!tunnelManager.hasVpnPermission())
                return@intent postSideEffect(
                    GlobalSideEffect.RequestVpnPermission(AppMode.VPN, tunnelConf)
                )
        }
        tunnelManager.startTunnel(tunnelConf)
    }

    fun postSideEffect(localSideEffect: LocalSideEffect) = intent {
        postSideEffect(localSideEffect)
    }

    fun setTheme(theme: Theme) = intent { appStateRepository.setTheme(theme) }

    fun setLocale(locale: String) = intent {
        appStateRepository.setLocale(locale)
        postSideEffect(GlobalSideEffect.ConfigChanged)
    }

    fun setPinLockEnabled(enabled: Boolean) = intent {
        if (!enabled) PinManager.clearPin()
        appStateRepository.setPinLockEnabled(enabled)
    }

    fun stopTunnel(tunnelConf: TunnelConf) = intent { tunnelManager.stopTunnel(tunnelConf.id) }

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
            AppMode.KERNEL -> if (!requestRoot()) return@intent
        }
        settingsRepository.save(state.settings.copy(appMode = appMode))
    }

    fun updateTopNavActions(actions: (@Composable () -> Unit)?) = intent {
        reduce { state.copy(topNavActions = actions) }
    }

    suspend fun postSideEffect(globalSideEffect: GlobalSideEffect) {
        globalEffectRepository.post(globalSideEffect)
    }

    fun authenticated() = intent { reduce { state.copy(isAuthorized = true) } }

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

    fun setWifiDetectionMethod(method: WifiDetectionMethod) = intent {
        when (method) {
            WifiDetectionMethod.ROOT -> if (!requestRoot()) return@intent
            WifiDetectionMethod.SHIZUKU -> return@intent requestShizuku()
            else -> Unit
        }
        settingsRepository.save(state.settings.copy(wifiDetectionMethod = method))
    }

    private fun requestShizuku() {
        Shizuku.addRequestPermissionResultListener(
            Shizuku.OnRequestPermissionResultListener { requestCode: Int, grantResult: Int ->
                if (grantResult != PERMISSION_GRANTED) return@OnRequestPermissionResultListener
                setWifiDetectionMethod(WifiDetectionMethod.SHIZUKU)
            }
        )
        try {
            if (Shizuku.checkSelfPermission() != PERMISSION_GRANTED) Shizuku.requestPermission(123)
        } catch (_: Exception) {
            intent {
                postSideEffect(
                    GlobalSideEffect.Snackbar(
                        StringValue.StringResource(R.string.shizuku_not_detected)
                    )
                )
            }
        }
    }

    private suspend fun requestRoot(): Boolean =
        withContext(ioDispatcher) {
            val accepted =
                try {
                    rootShell.get().start()
                    true
                } catch (_: Exception) {
                    false
                }
            val message =
                if (!accepted) StringValue.StringResource(R.string.error_root_denied)
                else StringValue.StringResource(R.string.root_accepted)
            intent { postSideEffect(GlobalSideEffect.Snackbar(message)) }
            accepted
        }
}
