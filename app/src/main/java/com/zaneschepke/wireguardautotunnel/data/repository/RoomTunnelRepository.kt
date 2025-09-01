package com.zaneschepke.wireguardautotunnel.data.repository

import com.zaneschepke.wireguardautotunnel.data.dao.TunnelConfigDao
import com.zaneschepke.wireguardautotunnel.data.mapper.TunnelConfigMapper
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.util.extensions.Tunnels
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RoomTunnelRepository(
    private val tunnelConfigDao: TunnelConfigDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TunnelRepository {

    override val flow =
        tunnelConfigDao.getAllFlow().flowOn(ioDispatcher).map {
            it.map(TunnelConfigMapper::toTunnelConf)
        }

    override suspend fun getAll(): Tunnels {
        return withContext(ioDispatcher) {
            tunnelConfigDao.getAll().map(TunnelConfigMapper::toTunnelConf)
        }
    }

    override suspend fun save(tunnelConf: TunnelConf) {
        withContext(ioDispatcher) {
            tunnelConfigDao.save(TunnelConfigMapper.toTunnelConfig(tunnelConf))
        }
    }

    override suspend fun saveAll(tunnelConfList: List<TunnelConf>) {
        withContext(ioDispatcher) {
            tunnelConfigDao.saveAll(tunnelConfList.map(TunnelConfigMapper::toTunnelConfig))
        }
    }

    override suspend fun updatePrimaryTunnel(tunnelConf: TunnelConf?) {
        withContext(ioDispatcher) {
            tunnelConfigDao.resetPrimaryTunnel()
            tunnelConf?.let { save(it.copy(isPrimaryTunnel = true)) }
        }
    }

    override suspend fun updateMobileDataTunnel(tunnelConf: TunnelConf?) {
        withContext(ioDispatcher) {
            tunnelConfigDao.resetMobileDataTunnel()
            tunnelConf?.let { save(it.copy(isMobileDataTunnel = true)) }
        }
    }

    override suspend fun updateEthernetTunnel(tunnelConf: TunnelConf?) {
        withContext(ioDispatcher) {
            tunnelConfigDao.resetEthernetTunnel()
            tunnelConf?.let { save(it.copy(isEthernetTunnel = true)) }
        }
    }

    override suspend fun delete(tunnelConf: TunnelConf) {
        withContext(ioDispatcher) {
            tunnelConfigDao.delete(TunnelConfigMapper.toTunnelConfig(tunnelConf))
        }
    }

    override suspend fun getById(id: Int): TunnelConf? {
        return withContext(ioDispatcher) {
            tunnelConfigDao.getById(id.toLong())?.let(TunnelConfigMapper::toTunnelConf)
        }
    }

    override suspend fun getActive(): Tunnels {
        return withContext(ioDispatcher) {
            tunnelConfigDao.getActive().map(TunnelConfigMapper::toTunnelConf)
        }
    }

    override suspend fun count(): Int {
        return withContext(ioDispatcher) { tunnelConfigDao.count().toInt() }
    }

    override suspend fun findByTunnelName(name: String): TunnelConf? {
        return withContext(ioDispatcher) {
            tunnelConfigDao.getByName(name)?.let(TunnelConfigMapper::toTunnelConf)
        }
    }

    override suspend fun findByTunnelNetworksName(name: String): Tunnels {
        return withContext(ioDispatcher) {
            tunnelConfigDao.findByTunnelNetworkName(name).map(TunnelConfigMapper::toTunnelConf)
        }
    }

    override suspend fun findByMobileDataTunnel(): Tunnels {
        return withContext(ioDispatcher) {
            tunnelConfigDao.findByMobileDataTunnel().map(TunnelConfigMapper::toTunnelConf)
        }
    }

    override suspend fun findPrimary(): Tunnels {
        return withContext(ioDispatcher) {
            tunnelConfigDao.findByPrimary().map(TunnelConfigMapper::toTunnelConf)
        }
    }

    override suspend fun delete(tunnels: List<TunnelConf>) {
        withContext(ioDispatcher) {
            tunnelConfigDao.delete(tunnels.map { TunnelConfigMapper.toTunnelConfig(it) })
        }
    }
}
