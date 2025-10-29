package com.zaneschepke.wireguardautotunnel.ui.state

import com.zaneschepke.wireguardautotunnel.domain.model.GeneralSettings
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState
import com.zaneschepke.wireguardautotunnel.ui.theme.Theme
import com.zaneschepke.wireguardautotunnel.util.LocaleUtil

data class SharedAppUiState(
    val isAppLoaded: Boolean = false,
    val theme: Theme = Theme.AUTOMATIC,
    val locale: String = LocaleUtil.OPTION_PHONE_LANGUAGE,
    val pinLockEnabled: Boolean = false,
    val tunnels: List<TunnelConfig> = emptyList(),
    val selectedTunnels: List<TunnelConfig> = emptyList(),
    val activeTunnels: Map<Int, TunnelState> = emptyMap(),
    val isPingEnabled: Boolean = false,
    val showPingStats: Boolean = false,
    val isPinVerified: Boolean = false,
    val isAutoTunnelActive: Boolean = false,
    val isLocationDisclosureShown: Boolean = false,
    val isBatteryOptimizationShown: Boolean = false,
    val proxyEnabled: Boolean = false,
    val settings: GeneralSettings = GeneralSettings(),
)
