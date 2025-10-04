package com.zaneschepke.wireguardautotunnel.domain.state

abstract class TunnelStatistics {
    open class PeerStats(
        val rxBytes: Long,
        val txBytes: Long,
        val latestHandshakeEpochMillis: Long,
        val resolvedEndpoint: String,
    ) {
        open fun copy(
            rxBytes: Long = this.rxBytes,
            txBytes: Long = this.txBytes,
            latestHandshakeEpochMillis: Long = this.latestHandshakeEpochMillis,
            resolvedEndpoint: String = this.resolvedEndpoint,
        ): PeerStats = PeerStats(rxBytes, txBytes, latestHandshakeEpochMillis, resolvedEndpoint)

        override fun toString(): String =
            "PeerStats(rxBytes=$rxBytes, txBytes=$txBytes, latestHandshakeEpochMillis=$latestHandshakeEpochMillis, resolvedEndpoint=$resolvedEndpoint)"
    }

    abstract fun peerStats(peerBase64: String): PeerStats?

    abstract fun isTunnelStale(): Boolean

    abstract fun getPeers(): Array<String>

    abstract fun rx(): Long

    abstract fun tx(): Long
}
