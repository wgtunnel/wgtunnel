package com.zaneschepke.wireguardautotunnel.ui.state

import com.zaneschepke.wireguardautotunnel.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf

data class TunnelUiState(
    val tunnel: TunnelConf? = null,
    val isActive: Boolean = false,
    val isLoading: Boolean = true,
    val isPingEnabled: Boolean = false,
    val isWildcardsEnabled: Boolean = false,
    val appMode: AppMode = AppMode.VPN,
)
