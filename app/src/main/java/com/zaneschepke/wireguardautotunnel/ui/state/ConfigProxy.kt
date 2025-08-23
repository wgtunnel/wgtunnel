package com.zaneschepke.wireguardautotunnel.ui.state

import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import org.amnezia.awg.config.Config

data class ConfigProxy(val peers: List<PeerProxy>, val `interface`: InterfaceProxy) {

    fun hasScripts(): Boolean {
        return `interface`.preUp.isNotBlank() ||
            `interface`.preDown.isNotBlank() ||
            `interface`.postUp.isNotBlank() ||
            `interface`.postDown.isNotBlank()
    }

    fun buildConfigs(): Pair<com.wireguard.config.Config, Config> {
        return Pair(
            com.wireguard.config.Config.Builder()
                .apply {
                    addPeers(peers.map { it.toWgPeer() })
                    setInterface(`interface`.toWgInterface())
                }
                .build(),
            Config.Builder()
                .apply {
                    addPeers(peers.map { it.toAmPeer() })
                    setInterface(`interface`.toAmInterface())
                }
                .build(),
        )
    }

    fun buildTunnelConfFromState(name: String, tunnelConf: TunnelConf?): TunnelConf {
        val (wg, am) = buildConfigs()
        return tunnelConf?.copyWithCallback(
            tunName = name,
            amQuick = am.toAwgQuickString(true, false),
            wgQuick = wg.toWgQuickString(true),
        )
            ?: TunnelConf(
                tunName = name,
                amQuick = am.toAwgQuickString(true, false),
                wgQuick = wg.toWgQuickString(true),
            )
    }

    companion object {
        fun from(amConfig: Config): ConfigProxy {
            return ConfigProxy(
                `interface` = InterfaceProxy.from(amConfig.`interface`),
                peers = amConfig.peers.map { PeerProxy.from(it) },
            )
        }
    }
}
