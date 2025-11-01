package com.zaneschepke.wireguardautotunnel.domain.model

data class LockdownSettings(
    val id: Long = 0L,
    val bypassLan: Boolean = false,
    val metered: Boolean = false,
    val dualStack: Boolean = false,
)
