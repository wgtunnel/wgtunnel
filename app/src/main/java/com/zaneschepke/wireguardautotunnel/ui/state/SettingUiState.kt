package com.zaneschepke.wireguardautotunnel.ui.state

import com.zaneschepke.wireguardautotunnel.domain.model.GeneralSettings
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf

data class SettingUiState(
    val settings: GeneralSettings = GeneralSettings(),
    val isLocalLoggingEnabled: Boolean = false,
    val remoteKey: String? = null,
    val isRemoteEnabled: Boolean = false,
    val isPinLockEnabled: Boolean = false,
    val showDetailedPingStats: Boolean = false,
    val isLoading: Boolean = true,
    val globalTunnelConf: TunnelConf? = null,
)
