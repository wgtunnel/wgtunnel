package com.zaneschepke.wireguardautotunnel.ui.state

import com.zaneschepke.wireguardautotunnel.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState

data class TunnelsUiState(
    val tunnels: List<TunnelConf> = emptyList(),
    val selectedTunnels: List<TunnelConf> = emptyList(),
    val activeTunnels: Map<Int, TunnelState> = emptyMap(),
    val isPingEnabled: Boolean = false,
    val appMode: AppMode = AppMode.VPN,
    val isWildcardsEnabled: Boolean = false,
    val showPingStats: Boolean = false,
    val stateInitialized: Boolean = false,
)
