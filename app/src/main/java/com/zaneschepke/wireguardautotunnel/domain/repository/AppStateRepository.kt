package com.zaneschepke.wireguardautotunnel.domain.repository

import com.zaneschepke.wireguardautotunnel.domain.model.AppState
import kotlinx.coroutines.flow.Flow

interface AppStateRepository {
    suspend fun isLocationDisclosureShown(): Boolean

    suspend fun setLocationDisclosureShown(shown: Boolean)

    suspend fun isBatteryOptimizationDisableShown(): Boolean

    suspend fun setBatteryOptimizationDisableShown(shown: Boolean)

    val flow: Flow<AppState>
}
