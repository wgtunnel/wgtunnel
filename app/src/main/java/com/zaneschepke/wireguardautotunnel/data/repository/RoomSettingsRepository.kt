package com.zaneschepke.wireguardautotunnel.data.repository

import com.zaneschepke.wireguardautotunnel.data.dao.GeneralSettingsDao
import com.zaneschepke.wireguardautotunnel.data.entity.GeneralSettings as Entity
import com.zaneschepke.wireguardautotunnel.data.mapper.toDomain
import com.zaneschepke.wireguardautotunnel.data.mapper.toEntity
import com.zaneschepke.wireguardautotunnel.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.domain.model.GeneralSettings as Domain
import com.zaneschepke.wireguardautotunnel.domain.repository.GeneralSettingRepository
import com.zaneschepke.wireguardautotunnel.ui.theme.Theme
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RoomSettingsRepository(
    private val settingsDao: GeneralSettingsDao,
    private val ioDispatcher: CoroutineDispatcher,
) : GeneralSettingRepository {
    override suspend fun upsert(generalSettings: Domain) {
        withContext(ioDispatcher) { settingsDao.upsert(generalSettings.toEntity()) }
    }

    override val flow =
        settingsDao
            .getGeneralSettingsFlow()
            .map { (it ?: Entity()).toDomain() }
            .flowOn(ioDispatcher)

    override suspend fun getGeneralSettings(): Domain {
        return withContext(ioDispatcher) {
            (settingsDao.getGeneralSettings() ?: Entity()).toDomain()
        }
    }

    override suspend fun updateTheme(theme: Theme) {
        withContext(ioDispatcher) { settingsDao.updateTheme(theme.name) }
    }

    override suspend fun updateLocale(locale: String) {
        withContext(ioDispatcher) { settingsDao.updateLocale(locale) }
    }

    override suspend fun updatePinLockEnabled(enabled: Boolean) {
        withContext(ioDispatcher) { settingsDao.updatePinLockEnabled(enabled) }
    }

    override suspend fun updateAppMode(appMode: AppMode) {
        withContext(ioDispatcher) { settingsDao.updateAppMode(appMode) }
    }
}
