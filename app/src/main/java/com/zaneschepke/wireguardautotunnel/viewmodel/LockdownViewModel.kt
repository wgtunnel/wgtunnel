package com.zaneschepke.wireguardautotunnel.viewmodel

import androidx.lifecycle.ViewModel
import com.zaneschepke.wireguardautotunnel.domain.repository.LockdownSettingsRepository
import com.zaneschepke.wireguardautotunnel.ui.state.LockdownSettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

@HiltViewModel
class LockdownViewModel
@Inject
constructor(private val lockdownSettingsRepository: LockdownSettingsRepository) :
    ContainerHost<LockdownSettingsUiState, Nothing>, ViewModel() {

    override val container =
        container<LockdownSettingsUiState, Nothing>(
            LockdownSettingsUiState(),
            buildSettings = { repeatOnSubscribedStopTimeout = 5000L },
        ) {
            lockdownSettingsRepository.flow.collect {
                reduce { state.copy(lockdownSettings = it, isLoading = false) }
            }
        }

    fun setBypassLan(to: Boolean) = intent {
        lockdownSettingsRepository.upsert(state.lockdownSettings.copy(bypassLan = to))
    }

    fun setMetered(to: Boolean) = intent {
        lockdownSettingsRepository.upsert(state.lockdownSettings.copy(metered = to))
    }

    fun setDualStack(to: Boolean) = intent {
        lockdownSettingsRepository.upsert(state.lockdownSettings.copy(dualStack = to))
    }
}
