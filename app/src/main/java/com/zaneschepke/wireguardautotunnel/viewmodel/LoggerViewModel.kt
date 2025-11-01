package com.zaneschepke.wireguardautotunnel.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.zaneschepke.logcatter.LogReader
import com.zaneschepke.wireguardautotunnel.BuildConfig
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.repository.GlobalEffectRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.MonitoringSettingsRepository
import com.zaneschepke.wireguardautotunnel.domain.sideeffect.GlobalSideEffect
import com.zaneschepke.wireguardautotunnel.ui.state.LoggerUiState
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.FileUtils
import com.zaneschepke.wireguardautotunnel.util.StringValue
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import timber.log.Timber

@HiltViewModel
class LoggerViewModel
@Inject
constructor(
    private val logReader: LogReader,
    private val monitoringRepository: MonitoringSettingsRepository,
    private val fileUtils: FileUtils,
    private val globalEffectRepository: GlobalEffectRepository,
) : ContainerHost<LoggerUiState, Nothing>, ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    override val container =
        container<LoggerUiState, Nothing>(
            LoggerUiState(),
            buildSettings = { repeatOnSubscribedStopTimeout = 5000L },
        ) {
            intent {
                monitoringRepository.flow
                    .onEach { reduce { state.copy(monitoringSettings = it) } }
                    .distinctUntilChangedBy { it.isLocalLogsEnabled }
                    .onEach { settings ->
                        if (settings.isLocalLogsEnabled) {
                            logReader.start()
                        } else {
                            logReader.stop()
                            logReader.deleteAndClearLogs()
                            reduce { state.copy(messages = emptyList()) }
                        }
                    }
                    .flatMapLatest { settings ->
                        if (settings.isLocalLogsEnabled) logReader.bufferedLogs else emptyFlow()
                    }
                    .catch { e -> Timber.e(e) }
                    .collect { logMessage ->
                        reduce {
                            state.copy(
                                messages =
                                    state.messages.toMutableList().apply {
                                        if (size >= MAX_LOG_SIZE) removeAt(0)
                                        add(logMessage)
                                    }
                            )
                        }
                    }
            }
        }

    suspend fun postSideEffect(globalSideEffect: GlobalSideEffect) {
        globalEffectRepository.post(globalSideEffect)
    }

    fun exportLogs(uri: Uri?) = intent {
        val result =
            fileUtils.createNewShareFile(
                "${Constants.BASE_LOG_FILE_NAME}_${BuildConfig.VERSION_NAME}_${BuildConfig.FLAVOR}.zip"
            )
        val onFailure = { action: Throwable ->
            Timber.e(action)
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
        result.onSuccess { file ->
            logReader.zipLogFiles(file.absolutePath)
            fileUtils.exportFile(file, uri, FileUtils.ZIP_FILE_MIME_TYPE).onFailure(onFailure)
        }
        result.onFailure(onFailure)
    }

    fun deleteLogs() = intent {
        monitoringRepository.upsert(state.monitoringSettings.copy(isLocalLogsEnabled = false))
        delay(1_000L)
        monitoringRepository.upsert(state.monitoringSettings.copy(isLocalLogsEnabled = true))
    }

    companion object {
        const val MAX_LOG_SIZE = 10_000L
    }
}
