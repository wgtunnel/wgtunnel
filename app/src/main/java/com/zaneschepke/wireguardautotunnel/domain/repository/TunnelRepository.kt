package com.zaneschepke.wireguardautotunnel.domain.repository

import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.util.extensions.Tunnels
import kotlinx.coroutines.flow.Flow

interface TunnelRepository {
	val flow: Flow<List<TunnelConf>>

	suspend fun getAll(): Tunnels

	suspend fun save(tunnelConf: TunnelConf)

	suspend fun updatePrimaryTunnel(tunnelConf: TunnelConf?)

	suspend fun updateMobileDataTunnel(tunnelConf: TunnelConf?)

	suspend fun updateEthernetTunnel(tunnelConf: TunnelConf?)

	suspend fun delete(tunnelConf: TunnelConf)

	suspend fun getById(id: Int): TunnelConf?

	suspend fun getActive(): Tunnels

	suspend fun count(): Int

	suspend fun findByTunnelName(name: String): TunnelConf?

	suspend fun findByTunnelNetworksName(name: String): Tunnels

	suspend fun findByMobileDataTunnel(): Tunnels

	suspend fun findPrimary(): Tunnels
}
