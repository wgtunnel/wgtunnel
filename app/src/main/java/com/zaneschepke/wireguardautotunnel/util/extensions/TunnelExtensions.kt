package com.zaneschepke.wireguardautotunnel.util.extensions

import androidx.compose.ui.graphics.Color
import com.wireguard.android.backend.BackendException
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendMode
import com.zaneschepke.wireguardautotunnel.domain.enums.HandshakeStatus
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.events.BackendError
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelStatistics
import com.zaneschepke.wireguardautotunnel.ui.theme.SilverTree
import com.zaneschepke.wireguardautotunnel.ui.theme.Straw
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.NumberUtils
import org.amnezia.awg.backend.Backend
import org.amnezia.awg.backend.Tunnel
import org.amnezia.awg.config.Config
import timber.log.Timber

fun TunnelStatistics.mapPeerStats(): Map<org.amnezia.awg.crypto.Key, TunnelStatistics.PeerStats?> {
    return this.getPeers().associateWith { key -> (this.peerStats(key)) }
}

fun TunnelStatistics.PeerStats.latestHandshakeSeconds(): Long? {
    return NumberUtils.getSecondsBetweenTimestampAndNow(this.latestHandshakeEpochMillis)
}

fun TunnelStatistics.PeerStats.handshakeStatus(): HandshakeStatus {
    // TODO add never connected status after duration
    return this.latestHandshakeSeconds().let {
        when {
            it == null -> HandshakeStatus.NOT_STARTED
            it <= HandshakeStatus.STALE_TIME_LIMIT_SEC -> HandshakeStatus.HEALTHY
            it > HandshakeStatus.STALE_TIME_LIMIT_SEC -> HandshakeStatus.STALE
            else -> {
                HandshakeStatus.UNKNOWN
            }
        }
    }
}

fun TunnelStatistics?.asColor(): Color {
    return this?.mapPeerStats()
        ?.map { it.value?.handshakeStatus() }
        ?.let { statuses ->
            when {
                statuses.all { it == HandshakeStatus.HEALTHY } -> SilverTree
                statuses.any { it == HandshakeStatus.STALE } -> Straw
                statuses.all { it == HandshakeStatus.NOT_STARTED } -> Color.Gray
                else -> Color.Gray
            }
        } ?: Color.Gray
}

fun Config.toWgQuickString(): String {
    val amQuick = toAwgQuickString(true, false)
    val lines = amQuick.lines().toMutableList()
    val linesIterator = lines.iterator()
    while (linesIterator.hasNext()) {
        val next = linesIterator.next()
        Constants.amProperties.forEach {
            if (next.startsWith(it, ignoreCase = true)) {
                linesIterator.remove()
            }
        }
    }
    return lines.joinToString(System.lineSeparator())
}

fun Config.defaultName(): String {
    return try {
        this.peers[0].endpoint.get().host
    } catch (e: Exception) {
        Timber.Forest.e(e)
        NumberUtils.generateRandomTunnelName()
    }
}

fun Backend.BackendMode.asBackendMode(): BackendMode {
    return when (val status = this) {
        is Backend.BackendMode.KillSwitch -> BackendMode.KillSwitch(status.allowedIps)
        else -> BackendMode.Inactive
    }
}

fun BackendMode.asAmBackendMode(): Backend.BackendMode {
    return when (val status = this) {
        is BackendMode.Inactive -> Backend.BackendMode.Inactive.INSTANCE
        is BackendMode.KillSwitch -> Backend.BackendMode.KillSwitch(status.allowedIps)
    }
}

fun Tunnel.State.asTunnelState(): TunnelStatus {
    return when (this) {
        Tunnel.State.DOWN -> TunnelStatus.Down
        Tunnel.State.UP -> TunnelStatus.Up
    }
}

fun BackendException.toBackendError(): BackendError {
    return when (this.reason) {
        BackendException.Reason.VPN_NOT_AUTHORIZED -> BackendError.Unauthorized
        BackendException.Reason.DNS_RESOLUTION_FAILURE -> BackendError.DNS
        BackendException.Reason.UNKNOWN_KERNEL_MODULE_NAME -> BackendError.KernelModuleName
        BackendException.Reason.WG_QUICK_CONFIG_ERROR_CODE -> BackendError.Config
        BackendException.Reason.TUNNEL_MISSING_CONFIG -> BackendError.Config
        BackendException.Reason.UNABLE_TO_START_VPN -> BackendError.NotAuthorized
        BackendException.Reason.TUN_CREATION_ERROR -> BackendError.NotAuthorized
        BackendException.Reason.GO_ACTIVATION_ERROR_CODE -> BackendError.Unknown
    }
}

fun org.amnezia.awg.backend.BackendException.toBackendError(): BackendError {
    return when (this.reason) {
        org.amnezia.awg.backend.BackendException.Reason.VPN_NOT_AUTHORIZED ->
            BackendError.Unauthorized
        org.amnezia.awg.backend.BackendException.Reason.DNS_RESOLUTION_FAILURE -> BackendError.DNS
        org.amnezia.awg.backend.BackendException.Reason.UNKNOWN_KERNEL_MODULE_NAME ->
            BackendError.KernelModuleName
        org.amnezia.awg.backend.BackendException.Reason.AWG_QUICK_CONFIG_ERROR_CODE ->
            BackendError.Config
        org.amnezia.awg.backend.BackendException.Reason.TUNNEL_MISSING_CONFIG -> BackendError.Config
        org.amnezia.awg.backend.BackendException.Reason.UNABLE_TO_START_VPN ->
            BackendError.NotAuthorized
        org.amnezia.awg.backend.BackendException.Reason.TUN_CREATION_ERROR ->
            BackendError.NotAuthorized
        org.amnezia.awg.backend.BackendException.Reason.GO_ACTIVATION_ERROR_CODE ->
            BackendError.Unknown
        org.amnezia.awg.backend.BackendException.Reason.SERVICE_NOT_RUNNING ->
            BackendError.ServiceNotRunning
    }
}

fun com.wireguard.android.backend.Tunnel.State.asTunnelState(): TunnelStatus {
    return when (this) {
        com.wireguard.android.backend.Tunnel.State.DOWN -> TunnelStatus.Down
        com.wireguard.android.backend.Tunnel.State.UP -> TunnelStatus.Up
    }
}
