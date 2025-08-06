package com.zaneschepke.wireguardautotunnel.domain.state

import org.amnezia.awg.crypto.Key

abstract class TunnelStatistics {
    open class PeerStats(
        val rxBytes: Long,
        val txBytes: Long,
        val latestHandshakeEpochMillis: Long,
        val resolvedEndpoint: String,
    ) {
        // mimic data class copy
        open fun copy(
            rxBytes: Long = this.rxBytes,
            txBytes: Long = this.txBytes,
            latestHandshakeEpochMillis: Long = this.latestHandshakeEpochMillis,
            resolvedEndpoint: String = this.resolvedEndpoint,
        ): PeerStats = PeerStats(rxBytes, txBytes, latestHandshakeEpochMillis, resolvedEndpoint)

        // Manual toString: Format like data class
        override fun toString(): String =
            "PeerStats(rxBytes=$rxBytes, txBytes=$txBytes, latestHandshakeEpochMillis=$latestHandshakeEpochMillis, resolvedEndpoint=$resolvedEndpoint)"
    }

    abstract fun peerStats(peer: Key): PeerStats?

    abstract fun isTunnelStale(): Boolean

    abstract fun getPeers(): Array<Key>

    abstract fun rx(): Long

    abstract fun tx(): Long
}
