package com.zaneschepke.wireguardautotunnel.viewmodel

import android.net.Uri
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.lifecycle.ViewModel
import com.wireguard.android.util.RootShell
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.data.entity.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.data.model.WifiDetectionMethod
import com.zaneschepke.wireguardautotunnel.di.AppShell
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.enums.ConfigType
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.AppStateRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.GeneralSettingRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.GlobalEffectRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.domain.sideeffect.GlobalSideEffect
import com.zaneschepke.wireguardautotunnel.ui.sideeffect.LocalSideEffect
import com.zaneschepke.wireguardautotunnel.ui.state.ConfigProxy
import com.zaneschepke.wireguardautotunnel.ui.state.SharedAppUiState
import com.zaneschepke.wireguardautotunnel.ui.theme.Theme
import com.zaneschepke.wireguardautotunnel.util.FileUtils
import com.zaneschepke.wireguardautotunnel.util.LocaleUtil
import com.zaneschepke.wireguardautotunnel.util.StringValue
import com.zaneschepke.wireguardautotunnel.util.extensions.asStringValue
import com.zaneschepke.wireguardautotunnel.util.extensions.saveTunnelsUniquely
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.amnezia.awg.config.BadConfigException
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import rikka.shizuku.Shizuku
import timber.log.Timber
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
    private val fileUtils: FileUtils,
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
                tunnelManager.messageEvents.collect { (_, message) ->
                    postSideEffect(GlobalSideEffect.Snackbar(message.toStringValue()))
                }
            }
        }

    fun startTunnel(tunnelConf: TunnelConf) = intent {
        if (state.settings.appMode == AppMode.VPN) {
            if (!serviceManager.hasVpnPermission())
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

    fun saveConfigProxy(tunnelId: Int?, configProxy: ConfigProxy, tunnelName: String) = intent {
        if (state.tunnels.any { it.tunName == tunnelName && it.id != tunnelId })
            return@intent postSideEffect(
                GlobalSideEffect.Toast(StringValue.StringResource(R.string.tunnel_name_taken))
            )
        runCatching {
                val (wg, am) = configProxy.buildConfigs()
                val tunnelConf =
                    if (tunnelId == null) {
                        TunnelConf.tunnelConfFromQuick(am.toAwgQuickString(true, false), tunnelName)
                    } else {
                        val latestTunnel = state.tunnels.find { it.id == tunnelId }
                        latestTunnel?.copy(
                            tunName = tunnelName,
                            amQuick = am.toAwgQuickString(true, false),
                            wgQuick = wg.toWgQuickString(true),
                        )
                    }
                if (tunnelConf != null) {
                    tunnelRepository.save(tunnelConf)
                    postSideEffect(
                        GlobalSideEffect.Toast(
                            StringValue.StringResource(R.string.config_changes_saved)
                        )
                    )
                    postSideEffect(GlobalSideEffect.PopBackStack)
                }
            }
            .onFailure {
                Timber.e(it)
                val message =
                    when (it) {
                        is BadConfigException -> it.asStringValue()
                        is com.wireguard.config.BadConfigException -> it.asStringValue()
                        else -> StringValue.StringResource(R.string.unknown_error)
                    }
                postSideEffect(GlobalSideEffect.Snackbar(message))
            }
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

    fun toggleSelectAllTunnels() = intent {
        if (state.selectedTunnels.size != state.tunnels.size) {
            return@intent reduce { state.copy(selectedTunnels = state.tunnels.toSet()) }
        }
        reduce { state.copy(selectedTunnels = emptySet()) }
    }

    fun clearSelectedTunnels() = intent { reduce { state.copy(selectedTunnels = emptySet()) } }

    fun toggleSelectedTunnel(tunnelId: Int) = intent {
        reduce {
            state.copy(
                selectedTunnels =
                    state.selectedTunnels.toMutableSet().apply {
                        val removed = removeIf { it.id == tunnelId }
                        if (!removed) addAll(state.tunnels.filter { it.id == tunnelId })
                    }
            )
        }
    }

    fun deleteSelectedTunnels() = intent {
        val activeTunIds = tunnelManager.activeTunnels.firstOrNull()?.map { it.key }
        if (state.selectedTunnels.any { activeTunIds?.contains(it.id) == true })
            return@intent postSideEffect(
                GlobalSideEffect.Snackbar(
                    StringValue.StringResource(R.string.delete_active_message)
                )
            )
        tunnelRepository.delete(state.selectedTunnels.toList())
        clearSelectedTunnels()
    }

    fun copySelectedTunnel() = intent {
        val selected = state.selectedTunnels.firstOrNull() ?: return@intent
        val copy = TunnelConf.tunnelConfFromQuick(selected.amQuick, selected.tunName)
        tunnelRepository.saveTunnelsUniquely(listOf(copy), state.tunnels)
        clearSelectedTunnels()
    }

    fun exportSelectedTunnels(configType: ConfigType, uri: Uri?) = intent {
        val (files, shareFileName) =
            when (configType) {
                ConfigType.AM ->
                    Pair(
                        createAmFiles(state.selectedTunnels),
                        "am-export_${Instant.now().epochSecond}.zip",
                    )
                ConfigType.WG ->
                    Pair(
                        createWgFiles(state.selectedTunnels),
                        "wg-export_${Instant.now().epochSecond}.zip",
                    )
            }
        val onFailure = { action: Throwable ->
            intent {
                postSideEffect(
                    GlobalSideEffect.Toast(
                        StringValue.StringResource(
                            R.string.export_failed,
                            ": ${action.localizedMessage}",
                        )
                    )
                )
            }
            Unit
        }
        fileUtils
            .createNewShareFile(shareFileName)
            .onSuccess {
                fileUtils.zipAll(it, files).onFailure(onFailure)
                fileUtils.exportFile(it, uri, FileUtils.ZIP_FILE_MIME_TYPE).onFailure(onFailure)
                postSideEffect(
                    GlobalSideEffect.Snackbar(StringValue.StringResource(R.string.export_success))
                )
                clearSelectedTunnels()
            }
            .onFailure(onFailure)
    }

    suspend fun createWgFiles(tunnels: Collection<TunnelConf>): List<File> =
        withContext(ioDispatcher) {
            tunnels.mapNotNull { config ->
                if (config.wgQuick.isNotBlank()) {
                    fileUtils.createFile(config.tunName, config.wgQuick)
                } else null
            }
        }

    suspend fun createAmFiles(tunnels: Collection<TunnelConf>): List<File> =
        withContext(ioDispatcher) {
            tunnels.mapNotNull { config ->
                if (
                    config.amQuick != TunnelConfig.AM_QUICK_DEFAULT && config.amQuick.isNotBlank()
                ) {
                    fileUtils.createFile(config.tunName, config.amQuick)
                } else null
            }
        }
}
