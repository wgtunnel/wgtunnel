package com.zaneschepke.wireguardautotunnel.util.extensions

import com.wireguard.android.backend.BackendException
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendMode
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.events.*
import com.zaneschepke.wireguardautotunnel.util.NumberUtils
import com.zaneschepke.wireguardautotunnel.util.StringValue
import org.amnezia.awg.backend.Backend
import org.amnezia.awg.backend.Tunnel
import org.amnezia.awg.config.BadConfigException
import org.amnezia.awg.config.Config
import timber.log.Timber

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

fun Config.defaultName(): String {
    return try {
        this.peers[0].endpoint.get().host
    } catch (e: Exception) {
        Timber.e(e)
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
        BackendException.Reason.VPN_NOT_AUTHORIZED -> VpnUnauthorized()
        BackendException.Reason.DNS_RESOLUTION_FAILURE -> DnsFailure()
        BackendException.Reason.UNKNOWN_KERNEL_MODULE_NAME -> VpnUnauthorized()
        BackendException.Reason.WG_QUICK_CONFIG_ERROR_CODE -> InvalidConfig()
        BackendException.Reason.TUNNEL_MISSING_CONFIG -> InvalidConfig()
        BackendException.Reason.UNABLE_TO_START_VPN -> VpnUnauthorized()
        BackendException.Reason.TUN_CREATION_ERROR -> VpnUnauthorized()
        BackendException.Reason.GO_ACTIVATION_ERROR_CODE -> UnknownError()
    }
}

fun org.amnezia.awg.backend.BackendException.toBackendCoreException(): BackendCoreException {
    return when (this.reason) {
        org.amnezia.awg.backend.BackendException.Reason.VPN_NOT_AUTHORIZED -> VpnUnauthorized()
        org.amnezia.awg.backend.BackendException.Reason.DNS_RESOLUTION_FAILURE -> DnsFailure()
        org.amnezia.awg.backend.BackendException.Reason.UNKNOWN_KERNEL_MODULE_NAME ->
            VpnUnauthorized()
        org.amnezia.awg.backend.BackendException.Reason.AWG_QUICK_CONFIG_ERROR_CODE ->
            InvalidConfig()
        org.amnezia.awg.backend.BackendException.Reason.TUNNEL_MISSING_CONFIG -> InvalidConfig()
        org.amnezia.awg.backend.BackendException.Reason.UNABLE_TO_START_VPN -> VpnUnauthorized()
        org.amnezia.awg.backend.BackendException.Reason.TUN_CREATION_ERROR -> VpnUnauthorized()
        org.amnezia.awg.backend.BackendException.Reason.GO_ACTIVATION_ERROR_CODE -> UnknownError()
        org.amnezia.awg.backend.BackendException.Reason.SERVICE_NOT_RUNNING -> ServiceNotRunning()
        org.amnezia.awg.backend.BackendException.Reason.UAPI_UPDATE_FAILED -> UapiUpdateFailed()
    }
}

fun com.wireguard.android.backend.Tunnel.State.asTunnelState(): TunnelStatus {
    return when (this) {
        com.wireguard.android.backend.Tunnel.State.DOWN -> TunnelStatus.Down
        com.wireguard.android.backend.Tunnel.State.UP -> TunnelStatus.Up
    }
}
