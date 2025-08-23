package com.zaneschepke.wireguardautotunnel.ui.screens.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaneschepke.wireguardautotunnel.BuildConfig
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.di.MainDispatcher
import com.zaneschepke.wireguardautotunnel.domain.model.AppUpdate
import com.zaneschepke.wireguardautotunnel.domain.repository.UpdateRepository
import com.zaneschepke.wireguardautotunnel.util.FileUtils
import com.zaneschepke.wireguardautotunnel.util.StringValue
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class SupportViewModel
@Inject
constructor(
    private val updateRepository: UpdateRepository,
    private val fileUtils: FileUtils,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupportUiState())
    val uiState = _uiState.asStateFlow()

    fun handleUpdateCheck() =
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            updateRepository
                .checkForUpdate(BuildConfig.VERSION_NAME)
                .onSuccess { appUpdate ->
                    _uiState.update {
                        it.copy(
                            appUpdate = appUpdate.sanitized(),
                            error = null,
                            isUptoDate = appUpdate == null,
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(error = StringValue.StringResource(R.string.update_check_failed))
                    }
                }
            _uiState.update { it.copy(isLoading = false) }
        }

    private fun AppUpdate?.sanitized(): AppUpdate? {
        return this?.copy(releaseNotes = releaseNotes.substringBefore(CHANGELOG_START))
    }

    fun handleErrorShown() = _uiState.update { it.copy(error = null) }

    fun handleUpdateShown() = _uiState.update { it.copy(appUpdate = null) }

    fun handleDownloadAndInstallApk() =
        viewModelScope.launch {
            with(uiState.value) {
                if (appUpdate == null) return@launch
                if (appUpdate.apkUrl == null || appUpdate.apkFileName == null) return@launch
                _uiState.update { it.copy(isLoading = true) }
                updateRepository
                    .downloadApk(appUpdate.apkUrl, appUpdate.apkFileName) { progress ->
                        withContext(mainDispatcher) {
                            _uiState.update { it.copy(downloadProgress = progress) }
                        }
                    }
                    .onSuccess { apk ->
                        _uiState.update { it.copy(isLoading = false) }
                        fileUtils.installApk(apk)
                    }
                    .onFailure {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = StringValue.StringResource(R.string.update_download_failed),
                            )
                        }
                    }
            }
        }

    companion object {
        private const val CHANGELOG_START =
            "SHA-256 fingerprint for the 4096-bit signing certificate:"
    }
}
