package com.zaneschepke.wireguardautotunnel.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zaneschepke.wireguardautotunnel.data.entity.TunnelConfig
import com.zaneschepke.wireguardautotunnel.util.extensions.TunnelConfigs
import kotlinx.coroutines.flow.Flow

@Dao
interface TunnelConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(t: TunnelConfig)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveAll(t: TunnelConfigs)

    @Query("SELECT * FROM TunnelConfig WHERE id=:id") suspend fun getById(id: Long): TunnelConfig?

    @Query("SELECT * FROM TunnelConfig WHERE name=:name")
    suspend fun getByName(name: String): TunnelConfig?

    @Query("SELECT * FROM TunnelConfig WHERE is_Active=1") suspend fun getActive(): TunnelConfigs

    @Query("SELECT * FROM TunnelConfig") suspend fun getAll(): TunnelConfigs

    @Delete suspend fun delete(t: TunnelConfig)

    @Query("SELECT COUNT('id') FROM TunnelConfig") suspend fun count(): Long

    @Query("SELECT * FROM TunnelConfig WHERE tunnel_networks LIKE '%' || :name || '%'")
    suspend fun findByTunnelNetworkName(name: String): TunnelConfigs

    @Query("UPDATE TunnelConfig SET is_primary_tunnel = 0 WHERE is_primary_tunnel =1")
    suspend fun resetPrimaryTunnel()

    @Query("UPDATE TunnelConfig SET is_mobile_data_tunnel = 0 WHERE is_mobile_data_tunnel =1")
    suspend fun resetMobileDataTunnel()

    @Query("UPDATE TunnelConfig SET is_ethernet_tunnel = 0 WHERE is_ethernet_tunnel =1")
    suspend fun resetEthernetTunnel()

    @Query("SELECT * FROM TUNNELCONFIG WHERE is_primary_tunnel=1")
    suspend fun findByPrimary(): TunnelConfigs

    @Query("SELECT * FROM TUNNELCONFIG WHERE is_mobile_data_tunnel=1")
    suspend fun findByMobileDataTunnel(): TunnelConfigs

    @Query("SELECT * FROM tunnelconfig ORDER BY position ASC")
    fun getAllFlow(): Flow<MutableList<TunnelConfig>>
}
