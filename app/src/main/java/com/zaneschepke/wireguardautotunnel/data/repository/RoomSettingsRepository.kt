package com.zaneschepke.wireguardautotunnel.data.repository

import com.zaneschepke.wireguardautotunnel.data.dao.SettingsDao
import com.zaneschepke.wireguardautotunnel.data.entity.Settings
import com.zaneschepke.wireguardautotunnel.data.mapper.toAppSettings
import com.zaneschepke.wireguardautotunnel.data.mapper.toSettings
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.model.AppSettings
import com.zaneschepke.wireguardautotunnel.domain.repository.AppSettingRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RoomSettingsRepository(
    private val settingsDoa: SettingsDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AppSettingRepository {

    override suspend fun save(appSettings: AppSettings) {
        withContext(ioDispatcher) { settingsDoa.save(appSettings.toSettings()) }
    }

    override val flow =
        settingsDoa.getSettingsFlow().flowOn(ioDispatcher).map { it.toAppSettings() }

    override suspend fun get(): AppSettings {
        return withContext(ioDispatcher) {
            (settingsDoa.getAll().firstOrNull() ?: Settings()).toAppSettings()
        }
    }
}
