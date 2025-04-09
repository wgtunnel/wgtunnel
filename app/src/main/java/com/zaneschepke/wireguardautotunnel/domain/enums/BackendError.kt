package com.zaneschepke.wireguardautotunnel.domain.enums

import com.zaneschepke.wireguardautotunnel.R

sealed class BackendError : Exception() {
    data object DNS : BackendError()

    data object Unauthorized : BackendError()

    data object Config : BackendError()

    data object KernelModuleName : BackendError()

    data object InvalidConfig : BackendError()

    data object NotAuthorized : BackendError()

    data object ServiceNotRunning : BackendError()

    data object Unknown : BackendError()

    fun toStringRes() =
        when (this) {
            Config -> R.string.config_error
            DNS -> R.string.dns_resolve_error
            InvalidConfig -> R.string.invalid_config_error
            KernelModuleName -> R.string.kernel_name_error
            NotAuthorized,
            Unauthorized -> R.string.auth_error
            ServiceNotRunning -> R.string.service_running_error
            Unknown -> R.string.unknown_error
        }
}
