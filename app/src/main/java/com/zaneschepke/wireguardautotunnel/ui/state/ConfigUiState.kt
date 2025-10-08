package com.zaneschepke.wireguardautotunnel.ui.state

import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf

data class ConfigUiState(
    val unavailableNames: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val tunnel: TunnelConf? = null,
)
