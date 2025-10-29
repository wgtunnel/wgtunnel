package com.zaneschepke.wireguardautotunnel.domain.repository

import com.zaneschepke.wireguardautotunnel.domain.model.LockdownSettings
import kotlinx.coroutines.flow.Flow

interface LockdownSettingsRepository {
    suspend fun upsert(lockdownSettings: LockdownSettings)

    val flow: Flow<LockdownSettings>

    suspend fun getLockdownSettings(): LockdownSettings
}
