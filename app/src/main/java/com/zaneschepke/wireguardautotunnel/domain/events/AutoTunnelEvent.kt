package com.zaneschepke.wireguardautotunnel.domain.events

import androidx.annotation.Keep
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConfig

sealed class AutoTunnelEvent {
    @Keep data class Start(val tunnelConfig: TunnelConfig? = null) : AutoTunnelEvent()

    @Keep data object Stop : AutoTunnelEvent()

    @Keep data object DoNothing : AutoTunnelEvent()
}
