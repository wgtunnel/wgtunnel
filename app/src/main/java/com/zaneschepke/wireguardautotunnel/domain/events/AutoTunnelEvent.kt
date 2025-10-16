package com.zaneschepke.wireguardautotunnel.domain.events

import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConfig

sealed class AutoTunnelEvent {
    data class Start(val tunnelConfig: TunnelConfig? = null) : AutoTunnelEvent()

    data object Stop : AutoTunnelEvent()

    data object DoNothing : AutoTunnelEvent()
}
