package com.zaneschepke.wireguardautotunnel.ui.state

import com.zaneschepke.networkmonitor.ConnectivityState
import com.zaneschepke.wireguardautotunnel.domain.model.GeneralSettings
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf

data class AutoTunnelUiState(
    val autoTunnelActive: Boolean = false,
    val connectivityState: ConnectivityState? = null,
    val settings: GeneralSettings = GeneralSettings(),
    val isBatteryOptimizationShown: Boolean = false,
    val isLocationDisclosureShown: Boolean = false,
    val tunnels: List<TunnelConf> = emptyList(),
    val isLoading: Boolean = true,
)
