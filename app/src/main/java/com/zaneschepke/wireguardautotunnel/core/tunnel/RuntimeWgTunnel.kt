package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.wireguard.android.backend.Tunnel
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import kotlinx.coroutines.channels.Channel

class RuntimeWgTunnel(
    private val config: TunnelConf,
    private val stateChannel: Channel<Tunnel.State>,
) : Tunnel {

    override fun getName() = config.tunName

    override fun onStateChange(newState: Tunnel.State) {
        stateChannel.trySend(newState)
    }

    override fun isIpv4ResolutionPreferred() = config.isIpv4Preferred
}
