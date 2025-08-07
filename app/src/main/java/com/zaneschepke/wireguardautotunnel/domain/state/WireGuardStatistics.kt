package com.zaneschepke.wireguardautotunnel.domain.state

import com.wireguard.android.backend.Statistics
import org.amnezia.awg.crypto.Key

class WireGuardStatistics(private val statistics: Statistics) : TunnelStatistics() {
    override fun peerStats(peer: Key): PeerStats? {
        val key = com.wireguard.crypto.Key.fromBase64(peer.toBase64())
        val peerStats = statistics.peer(key)
        return peerStats?.let {
            PeerStats(
                txBytes = peerStats.txBytes,
                rxBytes = peerStats.rxBytes,
                latestHandshakeEpochMillis = peerStats.latestHandshakeEpochMillis,
                resolvedEndpoint = peerStats.resolvedEndpoint,
            )
        }
    }

    override fun isTunnelStale(): Boolean {
        return statistics.isStale
    }

    override fun getPeers(): Array<Key> {
        return statistics.peers().map { Key.fromBase64(it.toBase64()) }.toTypedArray()
    }

    override fun rx(): Long {
        return statistics.totalRx()
    }

    override fun tx(): Long {
        return statistics.totalTx()
    }
}
