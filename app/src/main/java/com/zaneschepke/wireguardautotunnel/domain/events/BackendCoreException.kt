package com.zaneschepke.wireguardautotunnel.domain.events

import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.util.StringValue

sealed class BackendCoreException : Exception() {
    data object DNS : BackendCoreException()

    data object Unauthorized : BackendCoreException()

    data object Config : BackendCoreException()

    data object KernelModuleName : BackendCoreException()

    data object NotAuthorized : BackendCoreException()

    data object ServiceNotRunning : BackendCoreException()

    data object Unknown : BackendCoreException()

    data object TunnelNameTooLong : BackendCoreException()

    data object UapiUpdateFailed : BackendCoreException()

    data class BounceFailed(val error: BackendCoreException) : BackendCoreException()

    fun toStringRes() =
        when (this) {
            Config -> R.string.config_error
            DNS -> R.string.dns_resolve_error
            KernelModuleName -> R.string.kernel_name_error
            NotAuthorized,
            Unauthorized -> R.string.auth_error
            ServiceNotRunning -> R.string.service_running_error
            Unknown -> R.string.unknown_error
            TunnelNameTooLong -> R.string.error_tunnel_name
            is BounceFailed -> R.string.bounce_failed_template
            UapiUpdateFailed -> R.string.active_tunnel_update_failed
        }

    fun toStringValue(): StringValue {
        return when (val backendError = this) {
            is BounceFailed ->
                StringValue.StringResource(
                    backendError.toStringRes(),
                    backendError.error.toStringRes(),
                )
            else -> StringValue.StringResource(backendError.toStringRes())
        }
    }
}
