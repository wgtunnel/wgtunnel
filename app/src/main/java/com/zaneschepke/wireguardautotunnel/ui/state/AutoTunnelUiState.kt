package com.zaneschepke.wireguardautotunnel.ui.state

import com.zaneschepke.networkmonitor.ConnectivityState
import com.zaneschepke.wireguardautotunnel.domain.model.GeneralSettings

data class AutoTunnelUiState(
    val autoTunnelActive: Boolean = false,
    val connectivityState: ConnectivityState? = null,
    val generalSettings: GeneralSettings = GeneralSettings(),
    val isBatteryOptimizationShown: Boolean = false,
    val isLocationDisclosureShown: Boolean = false,
    val stateInitialized: Boolean = false,
)
