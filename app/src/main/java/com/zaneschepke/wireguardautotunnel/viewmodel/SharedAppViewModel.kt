package com.zaneschepke.wireguardautotunnel.viewmodel

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import com.wireguard.android.backend.WgQuickBackend
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.domain.enums.ConfigType
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.domain.repository.*
import com.zaneschepke.wireguardautotunnel.domain.sideeffect.GlobalSideEffect
import com.zaneschepke.wireguardautotunnel.ui.sideeffect.LocalSideEffect
import com.zaneschepke.wireguardautotunnel.ui.state.SharedAppUiState
import com.zaneschepke.wireguardautotunnel.ui.theme.Theme
import com.zaneschepke.wireguardautotunnel.util.FileUtils
import com.zaneschepke.wireguardautotunnel.util.LocaleUtil
import com.zaneschepke.wireguardautotunnel.util.RootShellUtils
import com.zaneschepke.wireguardautotunnel.util.StringValue
import com.zaneschepke.wireguardautotunnel.util.extensions.QuickConfig
import com.zaneschepke.wireguardautotunnel.util.extensions.TunnelName
import com.zaneschepke.wireguardautotunnel.util.extensions.asStringValue
import com.zaneschepke.wireguardautotunnel.util.extensions.saveTunnelsUniquely
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.io.IOException
import java.net.URL
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.amnezia.awg.config.BadConfigException
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
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
    private val monitoringSettingsRepository: MonitoringSettingsRepository,
    private val rootShellUtils: RootShellUtils,
    private val fileUtils: FileUtils,
) : ContainerHost<SharedAppUiState, LocalSideEffect>, ViewModel() {

    val globalSideEffect = globalEffectRepository.flow

    override val container =
        container<SharedAppUiState, LocalSideEffect>(
            SharedAppUiState(),
            buildSettings = { repeatOnSubscribedStopTimeout = 5000L },
        ) {
            intent {
                combine(
                        tunnelRepository.userTunnelsFlow,
                        serviceManager.autoTunnelService.map { it != null },
                        settingsRepository.flow,
                        tunnelManager.activeTunnels,
                        monitoringSettingsRepository.flow,
                    ) { tunnels, autoTunnelActive, settings, activeTunnels, monitoring ->
                        state.copy(
                            theme = settings.theme,
                            locale = settings.locale ?: LocaleUtil.OPTION_PHONE_LANGUAGE,
                            pinLockEnabled = settings.isPinLockEnabled,
                            isAutoTunnelActive = autoTunnelActive,
                            settings = settings,
                            tunnels = tunnels,
                            activeTunnels = activeTunnels,
                            isPingEnabled = monitoring.isPingEnabled,
                            showPingStats = monitoring.showDetailedPingStats,
                            proxyEnabled =
                                settings.appMode == AppMode.PROXY ||
                                    settings.appMode == AppMode.LOCK_DOWN,
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
                appStateRepository.flow.collect {
                    reduce {
                        state.copy(
                            isLocationDisclosureShown = it.isLocationDisclosureShown,
                            isBatteryOptimizationShown = it.isBatteryOptimizationDisableShown,
                            shouldShowDonationSnackbar = it.shouldShowDonationSnackbar,
                        )
                    }
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
//            AppMode.KERNEL -> {
//                val accepted = rootShellUtils.requestRoot()
//                val message =
//                    if (!accepted) StringValue.StringResource(R.string.error_root_denied)
//                    else StringValue.StringResource(R.string.root_accepted)
//                postSideEffect(GlobalSideEffect.Snackbar(message))
//                if (!accepted) return@intent
//            }
            AppMode.KERNEL -> {
                val accepted = rootShellUtils.requestRoot()
                val message =
                    if (!accepted) StringValue.StringResource(R.string.error_root_denied)
                    else StringValue.StringResource(R.string.root_accepted)
                postSideEffect(GlobalSideEffect.Snackbar(message))
                if (!accepted) return@intent
                if (WgQuickBackend.hasKernelSupport())
                    Timber.i("Device supports kernel backend. WireGuard module is built in, switching to kernel backend.")
                else {
                    Timber.e("Device does not support kernel backend!")
                    intent {
                        postSideEffect(
                            GlobalSideEffect.Snackbar(
                                StringValue.StringResource(R.string.kernel_wireguard_unsupported)
                            )
                        )
                    }
                    return@intent
                }
            }
        }
        settingsRepository.upsert(state.settings.copy(appMode = appMode))
    }

    fun setShouldShowDonationSnackbar(to: Boolean) = intent {
        appStateRepository.setShouldShowDonationSnackbar(to)
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

    fun saveSortChanges(tunnels: List<TunnelConfig>) = intent {
        tunnelRepository.saveAll(tunnels.mapIndexed { index, conf -> conf.copy(position = index) })
        postSideEffect(
            GlobalSideEffect.Snackbar(StringValue.StringResource(R.string.config_changes_saved))
        )
        postSideEffect(GlobalSideEffect.PopBackStack)
    }

    fun importTunnelConfigs(configs: Map<QuickConfig, TunnelName>) = intent {
        try {
            val tunnelConfigs =
                configs.map { (config, name) -> TunnelConfig.tunnelConfFromQuick(config, name) }
            tunnelRepository.saveTunnelsUniquely(tunnelConfigs, state.tunnels)
        } catch (_: IOException) {
            postSideEffect(
                GlobalSideEffect.Snackbar(StringValue.StringResource(R.string.read_failed))
            )
        } catch (e: BadConfigException) {
            postSideEffect(GlobalSideEffect.Snackbar(e.asStringValue()))
        }
    }

    fun importFromClipboard(conf: String) {
        importTunnelConfigs(mapOf(conf to null))
    }

    fun importFromQr(conf: String) = intent { importFromClipboard(conf) }

    fun importFromUrl(url: String) = intent {
        runCatching {
                val url = URL(url)
                val uri = url.toURI().toString().toUri()
                importFromUri(uri)
            }
            .onFailure {
                postSideEffect(
                    GlobalSideEffect.Toast(
                        StringValue.StringResource(R.string.error_download_failed)
                    )
                )
            }
    }

    fun importFromUri(uri: Uri) = intent {
        fileUtils
            .readConfigsFromUri(uri)
            .onSuccess { configs -> importTunnelConfigs(configs) }
            .onFailure {
                val message =
                    when (it) {
                        is IOException -> StringValue.StringResource(R.string.error_download_failed)
                        else -> StringValue.StringResource(R.string.error_file_extension)
                    }
                postSideEffect(GlobalSideEffect.Toast(message))
            }
    }

    fun toggleSelectAllTunnels() = intent {
        if (state.selectedTunnels.size != state.tunnels.size) {
            return@intent reduce { state.copy(selectedTunnels = state.tunnels) }
        }
        reduce { state.copy(selectedTunnels = emptyList()) }
    }

    fun clearSelectedTunnels() = intent { reduce { state.copy(selectedTunnels = emptyList()) } }

    fun toggleSelectedTunnel(tunnelId: Int) = intent {
        reduce {
            state.copy(
                selectedTunnels =
                    state.selectedTunnels.toMutableList().apply {
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
        tunnelRepository.delete(state.selectedTunnels)
        clearSelectedTunnels()
    }

    fun copySelectedTunnel() = intent {
        val selected = state.selectedTunnels.firstOrNull() ?: return@intent
        val copy = TunnelConfig.tunnelConfFromQuick(selected.amQuick, selected.name)
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

    suspend fun createWgFiles(tunnels: Collection<TunnelConfig>): List<File> =
        tunnels.mapNotNull { config ->
            if (config.wgQuick.isNotBlank()) {
                fileUtils.createFile(config.name, config.wgQuick)
            } else null
        }

    suspend fun createAmFiles(tunnels: Collection<TunnelConfig>): List<File> =
        tunnels.mapNotNull { config ->
            if (config.amQuick.isNotBlank()) {
                fileUtils.createFile(config.name, config.amQuick)
            } else null
        }
}
