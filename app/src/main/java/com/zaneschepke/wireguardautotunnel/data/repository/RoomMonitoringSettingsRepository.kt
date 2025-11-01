package com.zaneschepke.wireguardautotunnel.data.repository

import com.zaneschepke.wireguardautotunnel.data.dao.MonitoringSettingsDao
import com.zaneschepke.wireguardautotunnel.data.entity.MonitoringSettings as Entity
import com.zaneschepke.wireguardautotunnel.data.mapper.toDomain
import com.zaneschepke.wireguardautotunnel.data.mapper.toEntity
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.model.MonitoringSettings as Domain
import com.zaneschepke.wireguardautotunnel.domain.repository.MonitoringSettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class RoomMonitoringSettingsRepository(
    private val monitoringSettingsDao: MonitoringSettingsDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : MonitoringSettingsRepository {
    override suspend fun upsert(monitoringSettings: Domain) {
        monitoringSettingsDao.upsert(monitoringSettings.toEntity())
    }

    override val flow: Flow<Domain>
        get() =
            monitoringSettingsDao
                .getMonitoringSettingsFlow()
                .map { (it ?: Entity()).toDomain() }
                .flowOn(ioDispatcher)

    override suspend fun getMonitoringSettings(): Domain {
        return (monitoringSettingsDao.getMonitoringSettings() ?: Entity()).toDomain()
    }
}
