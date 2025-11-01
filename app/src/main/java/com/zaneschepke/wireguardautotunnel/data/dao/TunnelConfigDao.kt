package com.zaneschepke.wireguardautotunnel.data.dao

import androidx.room.*
import com.zaneschepke.wireguardautotunnel.data.entity.TunnelConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface TunnelConfigDao {

    @Upsert suspend fun upsert(t: TunnelConfig)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveAll(t: List<TunnelConfig>)

    @Query("SELECT * FROM tunnel_config WHERE id=:id") suspend fun getById(id: Long): TunnelConfig?

    @Query("UPDATE tunnel_config SET is_Active = 0 WHERE is_Active = 1")
    suspend fun resetActiveTunnels()

    @Query("SELECT * FROM tunnel_config WHERE name=:name")
    suspend fun getByName(name: String): TunnelConfig?

    @Query("SELECT * FROM tunnel_config WHERE is_Active=1")
    suspend fun getActive(): List<TunnelConfig>

    @Query("SELECT * FROM tunnel_config") suspend fun getAll(): List<TunnelConfig>

    @Delete suspend fun delete(t: TunnelConfig)

    @Delete suspend fun delete(t: List<TunnelConfig>)

    @Query("SELECT COUNT('id') FROM tunnel_config") suspend fun count(): Long

    @Query("SELECT * FROM tunnel_config WHERE tunnel_networks LIKE '%' || :name || '%'")
    suspend fun findByTunnelNetworkName(name: String): List<TunnelConfig>

    @Query("UPDATE tunnel_config SET is_primary_tunnel = 0 WHERE is_primary_tunnel =1")
    suspend fun resetPrimaryTunnel()

    @Query("UPDATE tunnel_config SET is_mobile_data_tunnel = 0 WHERE is_mobile_data_tunnel =1")
    suspend fun resetMobileDataTunnel()

    @Query("UPDATE tunnel_config SET is_ethernet_tunnel = 0 WHERE is_ethernet_tunnel =1")
    suspend fun resetEthernetTunnel()

    @Query("SELECT * FROM tunnel_config WHERE is_primary_tunnel=1")
    suspend fun findByPrimary(): List<TunnelConfig>

    @Query("SELECT * FROM tunnel_config WHERE is_mobile_data_tunnel=1")
    suspend fun findByMobileDataTunnel(): List<TunnelConfig>

    @Query(
        """
    SELECT * FROM tunnel_config 
    ORDER BY 
        CASE WHEN is_primary_tunnel = 1 THEN 0 ELSE 1 END, 
        position ASC 
    LIMIT 1"""
    )
    suspend fun getDefaultTunnel(): TunnelConfig?

    @Query(
        """
    SELECT * FROM tunnel_config
    ORDER BY 
        CASE WHEN is_Active = 1 THEN 0 
             WHEN is_primary_tunnel = 1 THEN 1 
             ELSE 2 END, 
        position ASC 
    LIMIT 1"""
    )
    suspend fun getStartTunnel(): TunnelConfig?

    @Query("SELECT * FROM tunnel_config ORDER BY position")
    fun getAllFlow(): Flow<List<TunnelConfig>>

    @Query("SELECT * FROM tunnel_config WHERE name != :globalName ORDER BY position")
    fun getAllTunnelsExceptGlobal(
        globalName: String = TunnelConfig.GLOBAL_CONFIG_NAME
    ): Flow<List<TunnelConfig>>

    @Query("SELECT * FROM tunnel_config WHERE name = :globalName LIMIT 1")
    fun getGlobalTunnel(globalName: String = TunnelConfig.GLOBAL_CONFIG_NAME): Flow<TunnelConfig?>
}
