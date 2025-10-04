package com.zaneschepke.wireguardautotunnel.domain.state

import com.wireguard.android.backend.Statistics
import com.wireguard.crypto.Key

class WireGuardStatistics(private val statistics: Statistics) : TunnelStatistics() {
    override fun peerStats(peerBase64: String): PeerStats? {
        val key = Key.fromBase64(peerBase64)
        val peerStats = statistics.peer(key)
        return peerStats?.let {
            PeerStats(
                rxBytes = it.rxBytes,
                txBytes = it.txBytes,
                latestHandshakeEpochMillis = it.latestHandshakeEpochMillis,
                resolvedEndpoint = it.resolvedEndpoint,
            )
        }
    }

    override fun isTunnelStale(): Boolean {
        return statistics.isStale
    }

    override fun getPeers(): Array<String> {
        return statistics.peers().map { it.toBase64() }.toTypedArray()
    }

    override fun rx(): Long {
        return statistics.totalRx()
    }

    override fun tx(): Long {
        return statistics.totalTx()
    }
}
