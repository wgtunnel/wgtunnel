package com.zaneschepke.wireguardautotunnel.ui.state

import org.amnezia.awg.config.Config

data class ConfigProxy(
    val peers: List<PeerProxy> = emptyList(),
    val `interface`: InterfaceProxy = InterfaceProxy(),
) {

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

    companion object {
        fun from(amConfig: Config): ConfigProxy {
            return ConfigProxy(
                `interface` = InterfaceProxy.from(amConfig.`interface`),
                peers = amConfig.peers.map { PeerProxy.from(it) },
            )
        }
    }
}
