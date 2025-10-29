package com.zaneschepke.wireguardautotunnel.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.zaneschepke.wireguardautotunnel.data.entity.LockdownSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface LockdownSettingsDao {
    @Query("SELECT * FROM lockdown_settings LIMIT 1")
    suspend fun getLockdownSettings(): LockdownSettings?

    @Upsert suspend fun upsert(lockdownSettings: LockdownSettings)

    @Query("SELECT * FROM lockdown_settings LIMIT 1")
    fun getLockdownSettingsFlow(): Flow<LockdownSettings?>
}
