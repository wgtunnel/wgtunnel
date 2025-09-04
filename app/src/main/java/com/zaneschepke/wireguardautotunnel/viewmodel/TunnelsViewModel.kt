package com.zaneschepke.wireguardautotunnel.viewmodel

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.data.entity.TunnelConfig
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.enums.ConfigType
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.AppStateRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.GeneralSettingRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.GlobalEffectRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.domain.sideeffect.GlobalSideEffect
import com.zaneschepke.wireguardautotunnel.ui.state.ConfigProxy
import com.zaneschepke.wireguardautotunnel.ui.state.TunnelsUiState
import com.zaneschepke.wireguardautotunnel.util.FileUtils
import com.zaneschepke.wireguardautotunnel.util.StringValue
import com.zaneschepke.wireguardautotunnel.util.extensions.QuickConfig
import com.zaneschepke.wireguardautotunnel.util.extensions.TunnelName
import com.zaneschepke.wireguardautotunnel.util.extensions.asStringValue
import com.zaneschepke.wireguardautotunnel.util.extensions.saveTunnelsUniquely
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import org.amnezia.awg.config.BadConfigException
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.net.URL
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class TunnelsViewModel
@Inject
constructor(
    private val tunnelRepository: TunnelRepository,
    private val generalSettingRepository: GeneralSettingRepository,
    private val appStateRepository: AppStateRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val fileUtils: FileUtils,
    private val globalEffectRepository: GlobalEffectRepository,
    private val tunnelManager: TunnelManager,
) : ContainerHost<TunnelsUiState, Nothing>, ViewModel() {

    override val container =
        container<TunnelsUiState, Nothing>(
            TunnelsUiState(),
            buildSettings = { repeatOnSubscribedStopTimeout = 5000L },
        ) {
            combine(
                    tunnelRepository.flow,
                    generalSettingRepository.flow,
                    appStateRepository.flow,
                    tunnelManager.activeTunnels,
                ) { tunnels, settings, appState, activeTunnels ->
                    TunnelsUiState(
                        tunnels = tunnels,
                        activeTunnels = activeTunnels,
                        appMode = settings.appMode,
                        isPingEnabled = settings.isPingEnabled,
                        isWildcardsEnabled = settings.isWildcardsEnabled,
                        showPingStats = appState.showDetailedPingStats,
                        stateInitialized = true,
                    )
                }
                .collect { reduce { it } }
        }

    suspend fun postSideEffect(globalSideEffect: GlobalSideEffect) {
        globalEffectRepository.post(globalSideEffect)
    }

    fun exportSelectedTunnels(configType: ConfigType, uri: Uri?, selected: Set<TunnelConf>) =
        intent {
            val (files, shareFileName) =
                when (configType) {
                    ConfigType.AM ->
                        Pair(createAmFiles(selected), "am-export_${Instant.now().epochSecond}.zip")
                    ConfigType.WG ->
                        Pair(createWgFiles(selected), "wg-export_${Instant.now().epochSecond}.zip")
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

    fun saveSortChanges(tunnels: List<TunnelConf>) = intent {
        tunnelRepository.saveAll(tunnels.mapIndexed { index, conf -> conf.copy(position = index) })
        postSideEffect(
            GlobalSideEffect.Snackbar(StringValue.StringResource(R.string.config_changes_saved))
        )
        postSideEffect(GlobalSideEffect.PopBackStack)
    }

    fun importTunnelConfigs(configs: Map<QuickConfig, TunnelName>) = intent {
        try {
            val tunnelConfigs =
                configs.map { (config, name) -> TunnelConf.tunnelConfFromQuick(config, name) }
            tunnelRepository.saveTunnelsUniquely(tunnelConfigs, state.tunnels)
        } catch (_: IOException) {
            postSideEffect(
                GlobalSideEffect.Snackbar(StringValue.StringResource(R.string.read_failed))
            )
        } catch (e: BadConfigException) {
            postSideEffect(GlobalSideEffect.Snackbar(e.asStringValue()))
        }
    }

    fun setTunnelPingIp(ip: String, tunnelId: Int) = intent {
        val latestTunnel = state.tunnels.find { it.id == tunnelId }
        if (latestTunnel != null) {
            tunnelRepository.save(latestTunnel.copy(pingTarget = ip.ifBlank { null }))
        }
    }

    fun addTunnelNetwork(tunnelId: Int, ssid: String) = intent {
        val latestTunnel = state.tunnels.find { it.id == tunnelId }
        if (latestTunnel != null) {
            tunnelRepository.save(
                latestTunnel.copy(
                    tunnelNetworks = latestTunnel.tunnelNetworks.toMutableSet().apply { add(ssid) }
                )
            )
        }
    }

    fun removeTunnelNetwork(tunnelId: Int, ssid: String) = intent {
        val latestTunnel = state.tunnels.find { it.id == tunnelId }
        if (latestTunnel != null) {
            tunnelRepository.save(
                latestTunnel.copy(
                    tunnelNetworks =
                        latestTunnel.tunnelNetworks.toMutableSet().apply { remove(ssid) }
                )
            )
        }
    }

    fun setRestartOnPing(tunnelId: Int, to: Boolean) = intent {
        val latestTunnel = state.tunnels.find { it.id == tunnelId }
        if (latestTunnel != null) {
            tunnelRepository.save(latestTunnel.copy(restartOnPingFailure = to))
        }
    }

    fun togglePrimaryTunnel(tunnelId: Int) = intent {
        val latestTunnel = state.tunnels.find { it.id == tunnelId }
        if (latestTunnel != null) {
            val update = if (latestTunnel.isPrimaryTunnel) null else latestTunnel
            tunnelRepository.updatePrimaryTunnel(update)
        }
    }

    fun setMobileDataTunnel(tunnelId: Int, to: Boolean) = intent {
        val latestTunnel = state.tunnels.find { it.id == tunnelId }
        if (latestTunnel != null) {
            tunnelRepository.save(latestTunnel.copy(isMobileDataTunnel = to))
        }
    }

    fun setEthernetTunnel(tunnelId: Int, to: Boolean) = intent {
        val latestTunnel = state.tunnels.find { it.id == tunnelId }
        if (latestTunnel != null) {
            tunnelRepository.save(latestTunnel.copy(isEthernetTunnel = to))
        }
    }

    fun toggleIpv4Preferred(tunnelId: Int) = intent {
        val latestTunnel = state.tunnels.find { it.id == tunnelId }
        if (latestTunnel != null) {
            tunnelRepository.save(
                latestTunnel.copy(isIpv4Preferred = !latestTunnel.isIpv4Preferred)
            )
        }
    }

    fun importFromClipboard(conf: String) {
        importTunnelConfigs(mapOf(conf to null))
    }

    fun importFromQr(conf: String) = intent {
        importFromClipboard(conf)
        postSideEffect(GlobalSideEffect.PopBackStack)
    }

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
        tunnelRepository.delete(state.selectedTunnels.toList())
        clearSelectedTunnels()
    }

    fun copySelectedTunnel() = intent {
        val selected = state.selectedTunnels.firstOrNull() ?: return@intent
        val copy = TunnelConf.tunnelConfFromQuick(selected.amQuick, selected.tunName)
        tunnelRepository.saveTunnelsUniquely(listOf(copy), state.tunnels)
        clearSelectedTunnels()
    }
}
