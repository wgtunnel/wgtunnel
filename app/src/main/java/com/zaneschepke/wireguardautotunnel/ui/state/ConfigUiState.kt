package com.zaneschepke.wireguardautotunnel.ui.state

import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConfig

data class ConfigUiState(
    val unavailableNames: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val tunnel: TunnelConfig? = null,
)
