package com.zaneschepke.wireguardautotunnel.viewmodel

import androidx.lifecycle.ViewModel
import com.zaneschepke.wireguardautotunnel.ui.state.LicenseUiState
import com.zaneschepke.wireguardautotunnel.util.FileUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

@HiltViewModel
class LicenseViewModel @Inject constructor(private val fileUtils: FileUtils) :
    ContainerHost<LicenseUiState, Nothing>, ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    override val container =
        container<LicenseUiState, Nothing>(
            LicenseUiState(),
            buildSettings = { repeatOnSubscribedStopTimeout = 5000L },
        ) {
            intent {
                val licenses = fileUtils.readLibraryLicensesFromAssets()
                reduce { state.copy(isLoading = false, licenses = licenses) }
            }
        }
}
