package com.zaneschepke.wireguardautotunnel.data.repository

import com.zaneschepke.wireguardautotunnel.data.dao.TunnelConfigDao
import com.zaneschepke.wireguardautotunnel.data.mapper.toDomain
import com.zaneschepke.wireguardautotunnel.data.mapper.toEntity
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConfig as Domain
import com.zaneschepke.wireguardautotunnel.domain.repository.TunnelRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RoomTunnelRepository(
    private val tunnelConfigDao: TunnelConfigDao,
    private val ioDispatcher: CoroutineDispatcher,
) : TunnelRepository {

    override val flow =
        tunnelConfigDao.getAllFlow().flowOn(ioDispatcher).map {
            it.map { tunnelConfig -> tunnelConfig.toDomain() }
        }

    override val userTunnelsFlow =
        tunnelConfigDao.getAllTunnelsExceptGlobal().flowOn(ioDispatcher).map {
            it.map { tunnelConfig -> tunnelConfig.toDomain() }
        }

    override val globalTunnelFlow: Flow<Domain?> =
        tunnelConfigDao.getGlobalTunnel().flowOn(ioDispatcher).map { it?.toDomain() }

    override suspend fun getAll(): List<Domain> {
        return withContext(ioDispatcher) { tunnelConfigDao.getAll().map { it.toDomain() } }
    }

    override suspend fun save(tunnelConfig: Domain) {
        withContext(ioDispatcher) { tunnelConfigDao.upsert(tunnelConfig.toEntity()) }
    }

    override suspend fun saveAll(tunnelConfigList: List<Domain>) {
        withContext(ioDispatcher) {
            tunnelConfigDao.saveAll(
                tunnelConfigList.map { tunnelConfig -> tunnelConfig.toEntity() }
            )
        }
    }

    override suspend fun updatePrimaryTunnel(tunnelConfig: Domain?) {
        withContext(ioDispatcher) {
            tunnelConfigDao.resetPrimaryTunnel()
            tunnelConfig?.let { save(it.copy(isPrimaryTunnel = true)) }
        }
    }

    override suspend fun resetActiveTunnels() {
        withContext(ioDispatcher) { tunnelConfigDao.resetActiveTunnels() }
    }

    override suspend fun updateMobileDataTunnel(tunnelConfig: Domain?) {
        withContext(ioDispatcher) {
            tunnelConfigDao.resetMobileDataTunnel()
            tunnelConfig?.let { save(it.copy(isMobileDataTunnel = true)) }
        }
    }

    override suspend fun updateEthernetTunnel(tunnelConfig: Domain?) {
        withContext(ioDispatcher) {
            tunnelConfigDao.resetEthernetTunnel()
            tunnelConfig?.let { save(it.copy(isEthernetTunnel = true)) }
        }
    }

    override suspend fun delete(tunnelConfig: Domain) {
        withContext(ioDispatcher) { tunnelConfigDao.delete(tunnelConfig.toEntity()) }
    }

    override suspend fun getById(id: Int): Domain? {
        return withContext(ioDispatcher) { tunnelConfigDao.getById(id.toLong())?.toDomain() }
    }

    override suspend fun getActive(): List<Domain> {
        return withContext(ioDispatcher) { tunnelConfigDao.getActive().map { it.toDomain() } }
    }

    override suspend fun getDefaultTunnel(): Domain? {
        return withContext(ioDispatcher) { tunnelConfigDao.getDefaultTunnel()?.toDomain() }
    }

    override suspend fun getStartTunnel(): Domain? {
        return withContext(ioDispatcher) { tunnelConfigDao.getStartTunnel()?.toDomain() }
    }

    override suspend fun count(): Int {
        return withContext(ioDispatcher) { tunnelConfigDao.count().toInt() }
    }

    override suspend fun findByTunnelName(name: String): Domain? {
        return withContext(ioDispatcher) { tunnelConfigDao.getByName(name)?.toDomain() }
    }

    override suspend fun findByTunnelNetworksName(name: String): List<Domain> {
        return withContext(ioDispatcher) {
            tunnelConfigDao.findByTunnelNetworkName(name).map { it.toDomain() }
        }
    }

    override suspend fun findByMobileDataTunnel(): List<Domain> {
        return withContext(ioDispatcher) {
            tunnelConfigDao.findByMobileDataTunnel().map { it.toDomain() }
        }
    }

    override suspend fun findPrimary(): List<Domain> {
        return withContext(ioDispatcher) { tunnelConfigDao.findByPrimary().map { it.toDomain() } }
    }

    override suspend fun delete(tunnels: List<Domain>) {
        withContext(ioDispatcher) { tunnelConfigDao.delete(tunnels.map { it.toEntity() }) }
    }
}
