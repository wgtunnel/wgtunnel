package com.zaneschepke.wireguardautotunnel.data.repository

import com.zaneschepke.wireguardautotunnel.data.dao.ProxySettingsDao
import com.zaneschepke.wireguardautotunnel.data.entity.ProxySettings as Entity
import com.zaneschepke.wireguardautotunnel.data.mapper.toDomain
import com.zaneschepke.wireguardautotunnel.data.mapper.toEntity
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.model.ProxySettings as Domain
import com.zaneschepke.wireguardautotunnel.domain.repository.ProxySettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RoomProxySettingsRepository(
    private val proxySettingsDao: ProxySettingsDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ProxySettingsRepository {
    override suspend fun upsert(proxySettings: Domain) {
        withContext(ioDispatcher) { proxySettingsDao.upsert(proxySettings.toEntity()) }
    }

    override val flow =
        proxySettingsDao
            .getProxySettingsFlow()
            .map { (it ?: Entity()).toDomain() }
            .flowOn(ioDispatcher)

    override suspend fun getProxySettings(): Domain {
        return withContext(ioDispatcher) {
            (proxySettingsDao.getProxySettings() ?: Entity()).toDomain()
        }
    }
}
