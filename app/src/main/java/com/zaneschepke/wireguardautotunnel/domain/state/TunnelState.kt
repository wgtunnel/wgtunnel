package com.zaneschepke.wireguardautotunnel.domain.state

import com.zaneschepke.wireguardautotunnel.domain.enums.BackendMode
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import org.amnezia.awg.crypto.Key

data class TunnelState(
    val status: TunnelStatus = TunnelStatus.Down,
    val backendState: BackendMode = BackendMode.Inactive,
    val statistics: TunnelStatistics? = null,
    val pingStates: Map<Key, PingState>? = null,
    val handshakeSuccessLogs: Boolean? = null,
)
