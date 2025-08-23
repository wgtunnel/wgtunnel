package com.zaneschepke.wireguardautotunnel.data.dao

import androidx.room.*
import com.zaneschepke.wireguardautotunnel.data.entity.ProxySettings
import kotlinx.coroutines.flow.Flow

@Dao
interface ProxySettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(t: ProxySettings)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveAll(t: List<ProxySettings>)

    @Query("SELECT * FROM proxy_settings WHERE id=:id")
    suspend fun getById(id: Long): ProxySettings?

    @Query("SELECT * FROM proxy_settings") suspend fun getAll(): List<ProxySettings>

    @Query("SELECT * FROM proxy_settings LIMIT 1") fun getSettingsFlow(): Flow<ProxySettings>

    @Query("SELECT * FROM proxy_settings") fun getAllFlow(): Flow<List<ProxySettings>>

    @Delete suspend fun delete(t: ProxySettings)

    @Query("SELECT COUNT('id') FROM proxy_settings") suspend fun count(): Long
}
