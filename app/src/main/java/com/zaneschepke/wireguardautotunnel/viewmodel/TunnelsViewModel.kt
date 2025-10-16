package com.zaneschepke.wireguardautotunnel.viewmodel

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.domain.enums.ConfigType
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.domain.repository.GlobalEffectRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.MonitoringSettingsRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.domain.sideeffect.GlobalSideEffect
import com.zaneschepke.wireguardautotunnel.ui.state.TunnelsUiState
import com.zaneschepke.wireguardautotunnel.util.FileUtils
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
import org.amnezia.awg.config.BadConfigException
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

@HiltViewModel
class TunnelsViewModel
@Inject
constructor(
    private val tunnelRepository: TunnelRepository,
    private val monitoringRepository: MonitoringSettingsRepository,
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
                    tunnelRepository.userTunnelsFlow,
                    tunnelManager.activeTunnels,
                    monitoringRepository.flow,
                ) { tunnels, activeTunnels, monitoring ->
                    state.copy(
                        tunnels = tunnels,
                        activeTunnels = activeTunnels,
                        isPingEnabled = monitoring.isPingEnabled,
                        showPingStats = monitoring.showDetailedPingStats,
                        isLoading = false,
                    )
                }
                .collect { reduce { it } }
        }

    suspend fun postSideEffect(globalSideEffect: GlobalSideEffect) {
        globalEffectRepository.post(globalSideEffect)
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
