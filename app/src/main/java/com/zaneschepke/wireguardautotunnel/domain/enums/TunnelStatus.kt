package com.zaneschepke.wireguardautotunnel.domain.enums

sealed class TunnelStatus {

    data class Up(val startTime: Long) : TunnelStatus()

    data object Down : TunnelStatus()

    data object Stopping : TunnelStatus()

    data object Starting : TunnelStatus()

    fun isDown(): Boolean {
        return this == Down
    }

    fun isUp(): Boolean {
        return this is Up
    }

    fun isUpOrStarting(): Boolean {
        return this is Up || this == Starting
    }

    fun isDownOrStopping(): Boolean {
        return this == Down || this is Stopping
    }
}
