package com.zaneschepke.wireguardautotunnel.ui.state

import com.wireguard.config.Peer
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.util.extensions.joinAndTrim

data class PeerProxy(
    val publicKey: String = "",
    val preSharedKey: String = "",
    val persistentKeepalive: String = "",
    val endpoint: String = "",
    val allowedIps: String = TunnelConfig.ALL_IPS.joinAndTrim(),
) {
    fun toWgPeer(): Peer {
        return Peer.Builder()
            .apply {
                parsePublicKey(publicKey)
                if (preSharedKey.isNotBlank()) parsePreSharedKey(preSharedKey)
                if (persistentKeepalive.isNotBlank()) parsePersistentKeepalive(persistentKeepalive)
                parseEndpoint(endpoint)
                parseAllowedIPs(allowedIps)
            }
            .build()
    }

    fun toAmPeer(): org.amnezia.awg.config.Peer {
        return org.amnezia.awg.config.Peer.Builder()
            .apply {
                parsePublicKey(publicKey)
                if (preSharedKey.isNotBlank()) parsePreSharedKey(preSharedKey)
                if (persistentKeepalive.isNotBlank()) parsePersistentKeepalive(persistentKeepalive)
                parseEndpoint(endpoint)
                parseAllowedIPs(allowedIps)
            }
            .build()
    }

    fun isLanExcluded(): Boolean {
        return this.allowedIps.contains(TunnelConfig.LAN_BYPASS_ALLOWED_IPS.joinAndTrim())
    }

    fun includeLan(): PeerProxy {
        return this.copy(allowedIps = TunnelConfig.ALL_IPS.joinAndTrim())
    }

    fun excludeLan(): PeerProxy {
        return this.copy(allowedIps = TunnelConfig.LAN_BYPASS_ALLOWED_IPS.joinAndTrim())
    }

    companion object {
        fun from(peer: Peer): PeerProxy {
            return PeerProxy(
                publicKey = peer.publicKey.toBase64(),
                preSharedKey =
                    if (peer.preSharedKey.isPresent) {
                        peer.preSharedKey.get().toBase64().trim()
                    } else {
                        ""
                    },
                persistentKeepalive =
                    if (peer.persistentKeepalive.isPresent) {
                        peer.persistentKeepalive.get().toString().trim()
                    } else {
                        ""
                    },
                endpoint =
                    if (peer.endpoint.isPresent) {
                        peer.endpoint.get().toString().trim()
                    } else {
                        ""
                    },
                allowedIps = peer.allowedIps.joinToString(", ").trim(),
            )
        }

        fun from(peer: org.amnezia.awg.config.Peer): PeerProxy {
            return PeerProxy(
                publicKey = peer.publicKey.toBase64(),
                preSharedKey =
                    if (peer.preSharedKey.isPresent) {
                        peer.preSharedKey.get().toBase64().trim()
                    } else {
                        ""
                    },
                persistentKeepalive =
                    if (peer.persistentKeepalive.isPresent) {
                        peer.persistentKeepalive.get().toString().trim()
                    } else {
                        ""
                    },
                endpoint =
                    if (peer.endpoint.isPresent) {
                        peer.endpoint.get().toString().trim()
                    } else {
                        ""
                    },
                allowedIps = peer.allowedIps.joinToString(", ").trim(),
            )
        }
    }
}
