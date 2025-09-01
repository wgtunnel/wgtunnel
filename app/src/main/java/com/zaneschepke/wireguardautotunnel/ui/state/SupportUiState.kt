package com.zaneschepke.wireguardautotunnel.ui.state

import com.zaneschepke.wireguardautotunnel.domain.model.AppUpdate

data class SupportUiState(
    val appUpdate: AppUpdate? = null,
    val isLoading: Boolean = false,
    val downloadProgress: Float = 0f,
)
