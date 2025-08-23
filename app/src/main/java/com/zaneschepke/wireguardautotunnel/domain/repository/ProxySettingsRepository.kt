package com.zaneschepke.wireguardautotunnel.domain.repository

import com.zaneschepke.wireguardautotunnel.domain.model.AppProxySettings
import kotlinx.coroutines.flow.Flow

interface ProxySettingsRepository {
    suspend fun save(proxySettings: AppProxySettings)

    val flow: Flow<AppProxySettings>

    suspend fun get(): AppProxySettings
}
