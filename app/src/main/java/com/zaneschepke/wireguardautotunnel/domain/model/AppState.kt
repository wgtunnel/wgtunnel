package com.zaneschepke.wireguardautotunnel.domain.model

import com.zaneschepke.wireguardautotunnel.ui.theme.Theme

data class AppState(
    val isLocationDisclosureShown: Boolean,
    val isBatteryOptimizationDisableShown: Boolean,
    val isPinLockEnabled: Boolean,
    val expandedTunnelIds: List<Int>,
    val isLocalLogsEnabled: Boolean,
    val isRemoteControlEnabled: Boolean,
    val showDetailedPingStats: Boolean,
    val remoteKey: String?,
    val locale: String?,
    val theme: Theme,
)
