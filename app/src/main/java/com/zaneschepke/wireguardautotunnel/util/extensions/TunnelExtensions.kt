package com.zaneschepke.wireguardautotunnel.util.extensions

import androidx.compose.ui.graphics.Color
import com.wireguard.android.backend.BackendException
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendMode
import com.zaneschepke.wireguardautotunnel.domain.enums.HandshakeStatus
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.events.BackendCoreException
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelStatistics
import com.zaneschepke.wireguardautotunnel.ui.theme.SilverTree
import com.zaneschepke.wireguardautotunnel.ui.theme.Straw
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.NumberUtils
import com.zaneschepke.wireguardautotunnel.util.StringValue
import org.amnezia.awg.backend.Backend
import org.amnezia.awg.backend.Tunnel
import org.amnezia.awg.config.BadConfigException
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

fun BadConfigException.asStringValue(): StringValue {
    val reason =
        when (this.reason) {
            BadConfigException.Reason.INVALID_KEY -> R.string.invalid_key
            BadConfigException.Reason.INVALID_NUMBER -> R.string.invalid_number
            BadConfigException.Reason.INVALID_VALUE -> R.string.invalid_value
            BadConfigException.Reason.MISSING_ATTRIBUTE -> R.string.missing_attribute
            BadConfigException.Reason.MISSING_SECTION -> R.string.missing_section
            BadConfigException.Reason.SYNTAX_ERROR -> R.string.syntax_error
            BadConfigException.Reason.UNKNOWN_ATTRIBUTE -> R.string.unknown_attribute
            BadConfigException.Reason.UNKNOWN_SECTION -> R.string.unknown_section
        }
    val location = this.location.name
    return StringValue.StringResource(R.string.config_error_template, reason, location)
}

fun com.wireguard.config.BadConfigException.asStringValue(): StringValue {
    val reason =
        when (this.reason) {
            com.wireguard.config.BadConfigException.Reason.INVALID_KEY -> R.string.invalid_key
            com.wireguard.config.BadConfigException.Reason.INVALID_NUMBER -> R.string.invalid_number
            com.wireguard.config.BadConfigException.Reason.INVALID_VALUE -> R.string.invalid_value
            com.wireguard.config.BadConfigException.Reason.MISSING_ATTRIBUTE ->
                R.string.missing_attribute
            com.wireguard.config.BadConfigException.Reason.MISSING_SECTION ->
                R.string.missing_section
            com.wireguard.config.BadConfigException.Reason.SYNTAX_ERROR -> R.string.syntax_error
            com.wireguard.config.BadConfigException.Reason.UNKNOWN_ATTRIBUTE ->
                R.string.unknown_attribute
            com.wireguard.config.BadConfigException.Reason.UNKNOWN_SECTION ->
                R.string.unknown_section
        }
    val location = this.location.name
    return StringValue.StringResource(R.string.config_error_template, reason, location)
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

fun BackendException.toBackendCoreException(): BackendCoreException {
    return when (this.reason) {
        BackendException.Reason.VPN_NOT_AUTHORIZED -> BackendCoreException.Unauthorized
        BackendException.Reason.DNS_RESOLUTION_FAILURE -> BackendCoreException.DNS
        BackendException.Reason.UNKNOWN_KERNEL_MODULE_NAME -> BackendCoreException.KernelModuleName
        BackendException.Reason.WG_QUICK_CONFIG_ERROR_CODE -> BackendCoreException.Config
        BackendException.Reason.TUNNEL_MISSING_CONFIG -> BackendCoreException.Config
        BackendException.Reason.UNABLE_TO_START_VPN -> BackendCoreException.NotAuthorized
        BackendException.Reason.TUN_CREATION_ERROR -> BackendCoreException.NotAuthorized
        BackendException.Reason.GO_ACTIVATION_ERROR_CODE -> BackendCoreException.Unknown
    }
}

fun org.amnezia.awg.backend.BackendException.toBackendCoreException(): BackendCoreException {
    return when (this.reason) {
        org.amnezia.awg.backend.BackendException.Reason.VPN_NOT_AUTHORIZED ->
            BackendCoreException.Unauthorized
        org.amnezia.awg.backend.BackendException.Reason.DNS_RESOLUTION_FAILURE ->
            BackendCoreException.DNS
        org.amnezia.awg.backend.BackendException.Reason.UNKNOWN_KERNEL_MODULE_NAME ->
            BackendCoreException.KernelModuleName
        org.amnezia.awg.backend.BackendException.Reason.AWG_QUICK_CONFIG_ERROR_CODE ->
            BackendCoreException.Config
        org.amnezia.awg.backend.BackendException.Reason.TUNNEL_MISSING_CONFIG ->
            BackendCoreException.Config
        org.amnezia.awg.backend.BackendException.Reason.UNABLE_TO_START_VPN ->
            BackendCoreException.NotAuthorized
        org.amnezia.awg.backend.BackendException.Reason.TUN_CREATION_ERROR ->
            BackendCoreException.NotAuthorized
        org.amnezia.awg.backend.BackendException.Reason.GO_ACTIVATION_ERROR_CODE ->
            BackendCoreException.Unknown
        org.amnezia.awg.backend.BackendException.Reason.SERVICE_NOT_RUNNING ->
            BackendCoreException.ServiceNotRunning
    }
}

fun com.wireguard.android.backend.Tunnel.State.asTunnelState(): TunnelStatus {
    return when (this) {
        com.wireguard.android.backend.Tunnel.State.DOWN -> TunnelStatus.Down
        com.wireguard.android.backend.Tunnel.State.UP -> TunnelStatus.Up
    }
}
