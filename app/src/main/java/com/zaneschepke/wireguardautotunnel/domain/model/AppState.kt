package com.zaneschepke.wireguardautotunnel.domain.model

import com.zaneschepke.wireguardautotunnel.ui.theme.Theme

data class AppState(
    val isLocationDisclosureShown: Boolean = false,
    val isBatteryOptimizationDisableShown: Boolean = false,
    val isPinLockEnabled: Boolean = false,
    val expandedTunnelIds: List<Int> = emptyList(),
    val isLocalLogsEnabled: Boolean = false,
    val isRemoteControlEnabled: Boolean = false,
    val showDetailedPingStats: Boolean = false,
    val remoteKey: String? = null,
    val locale: String? = null,
    val theme: Theme = Theme.AUTOMATIC,
)
