package com.zaneschepke.wireguardautotunnel.ui.state

import LicenseFileEntry

data class LicenseUiState(
    val isLoading: Boolean = true,
    val licenses: List<LicenseFileEntry> = emptyList(),
)
