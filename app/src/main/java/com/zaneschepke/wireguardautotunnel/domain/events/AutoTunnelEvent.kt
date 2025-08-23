package com.zaneschepke.wireguardautotunnel.domain.events

import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf

sealed class AutoTunnelEvent {
    data class Start(val tunnelConf: TunnelConf? = null) : AutoTunnelEvent()

    data class Bounce(val configsPeerKeyResolvedMap: List<Pair<TunnelConf, Map<String, String?>>>) :
        AutoTunnelEvent()

    data object Stop : AutoTunnelEvent()

    data object DoNothing : AutoTunnelEvent()
}
