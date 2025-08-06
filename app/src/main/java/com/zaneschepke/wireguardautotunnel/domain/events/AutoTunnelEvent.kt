package com.zaneschepke.wireguardautotunnel.domain.events

import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf

sealed class AutoTunnelEvent {
    data class Start(val tunnelConf: TunnelConf? = null) : AutoTunnelEvent()

    data class Bounce(val configsPeerKeyResolvedMap: List<Triple<TunnelConf, Map<String, String?>, Int>>) : AutoTunnelEvent()

    data object Stop : AutoTunnelEvent()

    data object DoNothing : AutoTunnelEvent()

    data class StartKillSwitch(val allowedIps : List<String>) : AutoTunnelEvent()
    data object StopKillSwitch : AutoTunnelEvent()
}
