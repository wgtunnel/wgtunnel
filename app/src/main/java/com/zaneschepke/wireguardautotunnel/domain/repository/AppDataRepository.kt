package com.zaneschepke.wireguardautotunnel.domain.repository

import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf

interface AppDataRepository {
    suspend fun getPrimaryOrFirstTunnel(): TunnelConf?

    suspend fun getStartTunnelConfig(): TunnelConf?

    val settings: GeneralSettingRepository
    val tunnels: TunnelRepository
    val appState: AppStateRepository

    val proxySettings: ProxySettingsRepository
}
