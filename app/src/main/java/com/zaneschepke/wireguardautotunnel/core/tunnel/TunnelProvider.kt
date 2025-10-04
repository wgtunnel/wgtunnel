package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.zaneschepke.wireguardautotunnel.domain.enums.BackendMode
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.events.BackendCoreException
import com.zaneschepke.wireguardautotunnel.domain.events.BackendMessage
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.state.LogHealthState
import com.zaneschepke.wireguardautotunnel.domain.state.PingState
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelStatistics
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface TunnelProvider {
    /** Starts the specified tunnel configuration. */
    suspend fun startTunnel(tunnelConf: TunnelConf)

    /**
     * Stops the specified tunnel.
     *
     * @param tunnelId The tunnelConf to stop.
     */
    suspend fun stopTunnel(tunnelId: Int)

    suspend fun forceStopTunnel(tunnelId: Int)

    /** Stops all active tunnels. */
    suspend fun stopActiveTunnels()

    fun setBackendMode(backendMode: BackendMode)

    fun getBackendMode(): BackendMode

    suspend fun runningTunnelNames(): Set<String>

    fun handleDnsReresolve(tunnelConf: TunnelConf): Boolean

    fun getStatistics(tunnelId: Int): TunnelStatistics?

    val activeTunnels: StateFlow<Map<Int, TunnelState>>

    val errorEvents: SharedFlow<Pair<String, BackendCoreException>>

    val messageEvents: SharedFlow<Pair<String, BackendMessage>>

    suspend fun updateTunnelStatus(
        tunnelId: Int,
        status: TunnelStatus? = null,
        stats: TunnelStatistics? = null,
        pingStates: Map<String, PingState>? = null,
        logHealthState: LogHealthState? = null,
    )
}
