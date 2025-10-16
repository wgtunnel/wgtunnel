package com.zaneschepke.wireguardautotunnel.ui.state

import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConfig

data class TunnelUiState(
    val tunnel: TunnelConfig? = null,
    val isActive: Boolean = false,
    val isLoading: Boolean = true,
)
