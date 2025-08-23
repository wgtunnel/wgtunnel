package com.zaneschepke.wireguardautotunnel.domain.enums

sealed class BackendMode {
    data object Inactive : BackendMode()

    data class KillSwitch(val allowedIps: Set<String>) : BackendMode()
}
