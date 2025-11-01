package com.zaneschepke.wireguardautotunnel.data.repository

import com.zaneschepke.wireguardautotunnel.data.dao.GeneralSettingsDao
import com.zaneschepke.wireguardautotunnel.data.entity.GeneralSettings as Entity
import com.zaneschepke.wireguardautotunnel.data.mapper.toDomain
import com.zaneschepke.wireguardautotunnel.data.mapper.toEntity
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.model.GeneralSettings as Domain
import com.zaneschepke.wireguardautotunnel.domain.repository.GeneralSettingRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RoomSettingsRepository(
    private val settingsDoa: GeneralSettingsDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : GeneralSettingRepository {

    override suspend fun upsert(generalSettings: Domain) {
        withContext(ioDispatcher) { settingsDoa.upsert(generalSettings.toEntity()) }
    }

    override val flow =
        settingsDoa
            .getGeneralSettingsFlow()
            .map { (it ?: Entity()).toDomain() }
            .flowOn(ioDispatcher)

    override suspend fun getGeneralSettings(): Domain {
        return withContext(ioDispatcher) {
            (settingsDoa.getGeneralSettings() ?: Entity()).toDomain()
        }
    }
}
