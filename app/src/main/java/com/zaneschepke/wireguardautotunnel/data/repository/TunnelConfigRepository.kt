package com.zaneschepke.wireguardautotunnel.data.repository

import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.util.extensions.TunnelConfigs
import kotlinx.coroutines.flow.Flow

interface TunnelConfigRepository {
	fun getTunnelConfigsFlow(): Flow<TunnelConfigs>

	suspend fun getAll(): TunnelConfigs

	suspend fun save(tunnelConfig: TunnelConfig)

	suspend fun updatePrimaryTunnel(tunnelConfig: TunnelConfig?)

	suspend fun updateMobileDataTunnel(tunnelConfig: TunnelConfig?)

	suspend fun delete(tunnelConfig: TunnelConfig)

	suspend fun getById(id: Int): TunnelConfig?

	suspend fun getActive(): TunnelConfigs

	suspend fun count(): Int

	suspend fun findByTunnelName(name: String): TunnelConfig?

	suspend fun findByTunnelNetworksName(name: String): TunnelConfigs

	suspend fun findByMobileDataTunnel(): TunnelConfigs

	suspend fun findPrimary(): TunnelConfigs
}
