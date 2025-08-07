package com.zaneschepke.wireguardautotunnel.domain.state

import org.amnezia.awg.backend.Statistics
import org.amnezia.awg.crypto.Key

class AmneziaStatistics(private val statistics: Statistics) : TunnelStatistics() {
    override fun peerStats(peer: Key): PeerStats? {
        val key = Key.fromBase64(peer.toBase64())
        val stats = statistics.peer(key)
        return stats?.let {
            PeerStats(
                rxBytes = stats.rxBytes,
                txBytes = stats.txBytes,
                latestHandshakeEpochMillis = stats.latestHandshakeEpochMillis,
                resolvedEndpoint = stats.resolvedEndpoint,
            )
        }
    }

    override fun isTunnelStale(): Boolean {
        return statistics.isStale
    }

    override fun getPeers(): Array<Key> {
        return statistics.peers()
    }

    override fun rx(): Long {
        return statistics.totalRx()
    }

    override fun tx(): Long {
        return statistics.totalTx()
    }
}
