package com.zaneschepke.wireguardautotunnel.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.zaneschepke.wireguardautotunnel.data.entity.ProxySettings
import kotlinx.coroutines.flow.Flow

@Dao
interface ProxySettingsDao {
    @Upsert suspend fun upsert(proxySettings: ProxySettings)

    @Query("SELECT * FROM proxy_settings LIMIT 1") suspend fun getProxySettings(): ProxySettings?

    @Query("SELECT * FROM proxy_settings LIMIT 1") fun getProxySettingsFlow(): Flow<ProxySettings?>
}
