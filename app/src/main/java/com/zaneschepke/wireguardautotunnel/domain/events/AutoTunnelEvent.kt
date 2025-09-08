package com.zaneschepke.wireguardautotunnel.domain.events

import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf

sealed class AutoTunnelEvent {
    data class Start(val tunnelConf: TunnelConf? = null) : AutoTunnelEvent()

    data object Stop : AutoTunnelEvent()

    data object DoNothing : AutoTunnelEvent()
}
