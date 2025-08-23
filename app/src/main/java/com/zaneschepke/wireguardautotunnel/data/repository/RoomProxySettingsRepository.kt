package com.zaneschepke.wireguardautotunnel.data.repository

import com.zaneschepke.wireguardautotunnel.data.dao.ProxySettingsDao
import com.zaneschepke.wireguardautotunnel.data.entity.ProxySettings
import com.zaneschepke.wireguardautotunnel.data.mapper.ProxySettingsMapper
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.model.AppProxySettings
import com.zaneschepke.wireguardautotunnel.domain.repository.ProxySettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RoomProxySettingsRepository(
    private val proxySettingsDao: ProxySettingsDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ProxySettingsRepository {
    override suspend fun save(proxySettings: AppProxySettings) {
        withContext(ioDispatcher) { proxySettingsDao.save(ProxySettingsMapper.to(proxySettings)) }
    }

    override val flow =
        proxySettingsDao.getSettingsFlow().flowOn(ioDispatcher).map(ProxySettingsMapper::to)

    override suspend fun get(): AppProxySettings {
        return withContext(ioDispatcher) {
            ProxySettingsMapper.to(proxySettingsDao.getAll().firstOrNull() ?: ProxySettings())
        }
    }
}
