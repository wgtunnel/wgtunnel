package com.zaneschepke.wireguardautotunnel.viewmodel

import androidx.lifecycle.ViewModel
import com.zaneschepke.logcatter.LogReader
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.repository.AppStateRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.GlobalEffectRepository
import com.zaneschepke.wireguardautotunnel.domain.sideeffect.GlobalSideEffect
import com.zaneschepke.wireguardautotunnel.ui.state.LoggerUiState
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.FileUtils
import com.zaneschepke.wireguardautotunnel.util.StringValue
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
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
    private val appStateRepository: AppStateRepository,
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
                appStateRepository.flow
                    .map { it.isLocalLogsEnabled }
                    .distinctUntilChanged()
                    .onEach { enabled ->
                        if (enabled) {
                            logReader.start()
                        } else {
                            logReader.stop()
                            logReader.deleteAndClearLogs()
                            reduce { state.copy(messages = emptyList()) }
                        }
                    }
                    .flatMapLatest { enabled ->
                        if (enabled) logReader.bufferedLogs else emptyFlow()
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

    fun exportLogs() = intent {
        val result =
            fileUtils.createNewShareFile(
                "${Constants.BASE_LOG_FILE_NAME}-${Instant.now().epochSecond}.zip"
            )
        result.onSuccess { file -> postSideEffect(GlobalSideEffect.ShareFile(file)) }
        result.onFailure { error ->
            val message =
                error.message?.let { StringValue.DynamicString(it) }
                    ?: StringValue.StringResource(R.string.unknown_error)
            postSideEffect(GlobalSideEffect.Toast(message))
        }
    }

    fun deleteLogs() = intent {
        appStateRepository.setLocalLogsEnabled(false)
        delay(1_000L)
        appStateRepository.setLocalLogsEnabled(true)
    }

    companion object {
        const val MAX_LOG_SIZE = 10_000L
    }
}
