package com.zaneschepke.wireguardautotunnel.domain.repository

import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf

interface AppDataRepository {
	suspend fun getPrimaryOrFirstTunnel(): TunnelConf?

	suspend fun getStartTunnelConfig(): TunnelConf?

	val settings: AppSettingRepository
	val tunnels: TunnelRepository
	val appState: AppStateRepository
}
