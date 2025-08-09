package com.zaneschepke.wireguardautotunnel.domain.enums

sealed class BackendStatus {
    data object Inactive : BackendStatus()

    data object Active : BackendStatus()

    data class KillSwitch(val allowedIps: List<String>) : BackendStatus()
}
