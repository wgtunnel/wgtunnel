package com.zaneschepke.wireguardautotunnel.domain.events

import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.util.StringValue

sealed class BackendError : Exception() {
    data object DNS : BackendError()

    data object Unauthorized : BackendError()

    data object Config : BackendError()

    data object KernelModuleName : BackendError()

    data object NotAuthorized : BackendError()

    data object ServiceNotRunning : BackendError()

    data object Unknown : BackendError()

    data object TunnelNameTooLong : BackendError()

    data class BounceFailed(val error: BackendError) : BackendError()

    fun toStringRes() = when (this) {
        Config -> R.string.config_error
        DNS -> R.string.dns_resolve_error
        KernelModuleName -> R.string.kernel_name_error
        NotAuthorized,
        Unauthorized -> R.string.auth_error
        ServiceNotRunning -> R.string.service_running_error
        Unknown -> R.string.unknown_error
        TunnelNameTooLong -> R.string.error_tunnel_name
        is BounceFailed -> R.string.bounce_failed_template
    }

    fun toStringValue() : StringValue {
        return when (val backendError = this) {
            is BounceFailed -> StringValue.StringResource(backendError.toStringRes(),  backendError.error.toStringRes())
            else -> StringValue.StringResource(backendError.toStringRes())
        }
    }
}