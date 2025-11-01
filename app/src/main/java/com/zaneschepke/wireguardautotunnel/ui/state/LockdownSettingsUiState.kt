package com.zaneschepke.wireguardautotunnel.ui.state

import com.zaneschepke.wireguardautotunnel.domain.model.LockdownSettings

data class LockdownSettingsUiState(
    val lockdownSettings: LockdownSettings = LockdownSettings(),
    val isLoading: Boolean = true,
    val showSaveModal: Boolean = false,
)
