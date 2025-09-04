package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import kotlinx.coroutines.channels.Channel
import org.amnezia.awg.backend.Tunnel

class RuntimeAwgTunnel(
    private val tunnelConf: TunnelConf,
    private val stateChannel: Channel<Tunnel.State>,
) : Tunnel {

    override fun getName() = tunnelConf.tunName

    override fun onStateChange(newState: Tunnel.State) {
        stateChannel.trySend(newState)
    }

    override fun isIpv4ResolutionPreferred() = tunnelConf.isIpv4Preferred
}
