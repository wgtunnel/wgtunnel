package com.zaneschepke.wireguardautotunnel.data.dao

import androidx.room.*
import com.zaneschepke.wireguardautotunnel.data.entity.TunnelConfig
import com.zaneschepke.wireguardautotunnel.util.extensions.TunnelConfigs
import kotlinx.coroutines.flow.Flow

@Dao
interface TunnelConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(t: TunnelConfig)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveAll(t: TunnelConfigs)

    @Query("SELECT * FROM TunnelConfig WHERE id=:id") suspend fun getById(id: Long): TunnelConfig?

    @Query("UPDATE TunnelConfig SET is_Active = 0 WHERE is_Active = 1")
    suspend fun resetActiveTunnels()

    @Query("SELECT * FROM TunnelConfig WHERE name=:name")
    suspend fun getByName(name: String): TunnelConfig?

    @Query("SELECT * FROM TunnelConfig WHERE is_Active=1") suspend fun getActive(): TunnelConfigs

    @Query("SELECT * FROM TunnelConfig") suspend fun getAll(): TunnelConfigs

    @Delete suspend fun delete(t: TunnelConfig)

    @Delete suspend fun delete(t: TunnelConfigs)

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

    @Query(
        """
    SELECT * FROM TunnelConfig 
    ORDER BY 
        CASE WHEN is_primary_tunnel = 1 THEN 0 ELSE 1 END, 
        position ASC 
    LIMIT 1"""
    )
    suspend fun getDefaultTunnel(): TunnelConfig?

    @Query(
        """
    SELECT * FROM TunnelConfig 
    ORDER BY 
        CASE WHEN is_Active = 1 THEN 0 
             WHEN is_primary_tunnel = 1 THEN 1 
             ELSE 2 END, 
        position ASC 
    LIMIT 1"""
    )
    suspend fun getStartTunnel(): TunnelConfig?

    @Query("SELECT * FROM tunnelconfig ORDER BY position")
    fun getAllFlow(): Flow<List<TunnelConfig>>
}
