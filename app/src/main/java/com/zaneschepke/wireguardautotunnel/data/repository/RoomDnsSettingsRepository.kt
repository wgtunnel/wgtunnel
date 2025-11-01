package com.zaneschepke.wireguardautotunnel.data.repository

import com.zaneschepke.wireguardautotunnel.data.dao.DnsSettingsDao
import com.zaneschepke.wireguardautotunnel.data.entity.DnsSettings as Entity
import com.zaneschepke.wireguardautotunnel.data.mapper.toDomain
import com.zaneschepke.wireguardautotunnel.data.mapper.toEntity
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.model.DnsSettings as Domain
import com.zaneschepke.wireguardautotunnel.domain.repository.DnsSettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class RoomDnsSettingsRepository(
    private val dnsSettingsDao: DnsSettingsDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DnsSettingsRepository {
    override suspend fun upsert(dnsSettings: Domain) {
        dnsSettingsDao.upsert(dnsSettings.toEntity())
    }

    override val flow: Flow<Domain>
        get() =
            dnsSettingsDao
                .getDnsSettingsFlow()
                .map { (it ?: Entity()).toDomain() }
                .flowOn(ioDispatcher)

    override suspend fun getDnsSettings(): Domain {
        return (dnsSettingsDao.getDnsSettings() ?: Entity()).toDomain()
    }
}
