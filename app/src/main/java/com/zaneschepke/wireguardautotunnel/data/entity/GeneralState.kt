package com.zaneschepke.wireguardautotunnel.data.entity

import com.zaneschepke.wireguardautotunnel.ui.theme.Theme

data class GeneralState(
    val isLocationDisclosureShown: Boolean = LOCATION_DISCLOSURE_SHOWN_DEFAULT,
    val isBatteryOptimizationDisableShown: Boolean = BATTERY_OPTIMIZATION_DISABLE_SHOWN_DEFAULT,
    val isPinLockEnabled: Boolean = PIN_LOCK_ENABLED_DEFAULT,
    val expandedTunnelIds: List<Int> = emptyList(),
    val isLocalLogsEnabled: Boolean = IS_LOGS_ENABLED_DEFAULT,
    val isRemoteControlEnabled: Boolean = IS_REMOTE_CONTROL_ENABLED,
    val showDetailedPingStats: Boolean = SHOW_DETAILED_PING_STATS_DEFAULT,
    val remoteKey: String? = null,
    val locale: String? = null,
    val theme: Theme = Theme.AUTOMATIC,
) {

    companion object {
        const val LOCATION_DISCLOSURE_SHOWN_DEFAULT = false
        const val BATTERY_OPTIMIZATION_DISABLE_SHOWN_DEFAULT = false
        const val PIN_LOCK_ENABLED_DEFAULT = false
        const val IS_LOGS_ENABLED_DEFAULT = false
        const val IS_REMOTE_CONTROL_ENABLED = false
        const val SHOW_DETAILED_PING_STATS_DEFAULT = false
    }
}
