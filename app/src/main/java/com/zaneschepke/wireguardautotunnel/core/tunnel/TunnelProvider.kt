package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendState
import kotlinx.coroutines.flow.StateFlow

interface TunnelProvider {

	suspend fun activeTunnels(): StateFlow<List<TunnelConf>>

	suspend fun startTunnel(tunnelConf: TunnelConf)

	suspend fun stopTunnel(tunnelConf: TunnelConf? = null)

	suspend fun bounceTunnel(tunnelConf: TunnelConf)

	suspend fun setBackendState(backendState: BackendState, allowedIps: Collection<String>)

	suspend fun runningTunnelNames(): Set<String>

	companion object {
		const val CHECK_INTERVAL = 1_000L
	}
}
