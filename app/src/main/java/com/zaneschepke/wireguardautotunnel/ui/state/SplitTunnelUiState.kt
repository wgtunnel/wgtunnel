package com.zaneschepke.wireguardautotunnel.ui.state

import com.zaneschepke.wireguardautotunnel.domain.model.InstalledPackage
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf

data class SplitTunnelUiState(
    val installedPackages: List<InstalledPackage> = emptyList(),
    val stateInitialized: Boolean = false,
    val tunnels: List<TunnelConf> = emptyList(),
)
