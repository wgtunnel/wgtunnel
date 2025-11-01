package com.zaneschepke.wireguardautotunnel.domain.repository

import com.zaneschepke.wireguardautotunnel.domain.model.GeneralSettings
import kotlinx.coroutines.flow.Flow

interface GeneralSettingRepository {
    suspend fun upsert(generalSettings: GeneralSettings)

    val flow: Flow<GeneralSettings>

    suspend fun getGeneralSettings(): GeneralSettings
}
