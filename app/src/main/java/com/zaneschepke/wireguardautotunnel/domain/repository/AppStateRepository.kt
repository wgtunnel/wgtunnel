package com.zaneschepke.wireguardautotunnel.domain.repository

import com.zaneschepke.wireguardautotunnel.domain.model.AppState
import com.zaneschepke.wireguardautotunnel.ui.theme.Theme
import kotlinx.coroutines.flow.Flow

interface AppStateRepository {
    suspend fun isLocationDisclosureShown(): Boolean

    suspend fun setLocationDisclosureShown(shown: Boolean)

    suspend fun isPinLockEnabled(): Boolean

    suspend fun setPinLockEnabled(enabled: Boolean)

    suspend fun isBatteryOptimizationDisableShown(): Boolean

    suspend fun setBatteryOptimizationDisableShown(shown: Boolean)

    suspend fun setTunnelExpanded(id: Int)

    suspend fun removeTunnelExpanded(id: Int)

    suspend fun setTheme(theme: Theme)

    suspend fun getTheme(): Theme

    suspend fun isLocalLogsEnabled(): Boolean

    suspend fun setLocalLogsEnabled(enabled: Boolean)

    suspend fun setLocale(localeTag: String)

    suspend fun getLocale(): String?

    suspend fun setIsRemoteControlEnabled(enabled: Boolean)

    suspend fun isRemoteControlEnabled(): Boolean

    suspend fun setRemoteKey(key: String)

    suspend fun getRemoteKey(): String?

    suspend fun setShowDetailedPingStats(showDetailedPing: Boolean)

    suspend fun getShowDetailedPing(): Boolean

    val flow: Flow<AppState>
}
