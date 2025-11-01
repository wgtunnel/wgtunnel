package com.zaneschepke.wireguardautotunnel.data.repository

import com.zaneschepke.wireguardautotunnel.data.dao.LockdownSettingsDao
import com.zaneschepke.wireguardautotunnel.data.entity.LockdownSettings as Entity
import com.zaneschepke.wireguardautotunnel.data.mapper.toDomain
import com.zaneschepke.wireguardautotunnel.data.mapper.toEntity
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.model.LockdownSettings as Domain
import com.zaneschepke.wireguardautotunnel.domain.repository.LockdownSettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RoomLockdownSettingsRepository(
    private val lockdownSettingsDao: LockdownSettingsDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : LockdownSettingsRepository {
    override suspend fun upsert(lockdownSettings: Domain) {
        withContext(ioDispatcher) { lockdownSettingsDao.upsert(lockdownSettings.toEntity()) }
    }

    override val flow =
        lockdownSettingsDao
            .getLockdownSettingsFlow()
            .map { (it ?: Entity()).toDomain() }
            .flowOn(ioDispatcher)

    override suspend fun getLockdownSettings(): Domain {
        return withContext(ioDispatcher) {
            (lockdownSettingsDao.getLockdownSettings() ?: Entity()).toDomain()
        }
    }
}
