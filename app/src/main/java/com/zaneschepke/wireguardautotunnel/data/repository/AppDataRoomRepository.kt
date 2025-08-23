package com.zaneschepke.wireguardautotunnel.data.repository

import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.*
import javax.inject.Inject

class AppDataRoomRepository
@Inject
constructor(
    override val settings: AppSettingRepository,
    override val tunnels: TunnelRepository,
    override val appState: AppStateRepository,
    override val proxySettings: ProxySettingsRepository,
) : AppDataRepository {

    override suspend fun getPrimaryOrFirstTunnel(): TunnelConf? {
        return tunnels.findPrimary().firstOrNull() ?: tunnels.getAll().firstOrNull()
    }

    override suspend fun getStartTunnelConfig(): TunnelConf? {
        tunnels.getActive().let {
            if (it.isNotEmpty()) return it.first()
            return getPrimaryOrFirstTunnel()
        }
    }
}
