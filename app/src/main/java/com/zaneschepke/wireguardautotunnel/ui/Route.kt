package com.zaneschepke.wireguardautotunnel.ui

import kotlinx.serialization.Serializable

sealed class Route {
    @Serializable data object Support : Route()

    @Serializable data object Settings : Route()

    @Serializable data object SettingsAdvanced : Route()

    @Serializable data object AutoTunnel : Route()

    @Serializable data object AutoTunnelAdvanced : Route()

    @Serializable data object WifiDetectionMethod : Route()

    @Serializable data object LocationDisclosure : Route()

    @Serializable data object Appearance : Route()

    @Serializable data object Display : Route()

    @Serializable data object KillSwitch : Route()

    @Serializable data object Language : Route()

    @Serializable data object Main : Route()

    @Serializable data class TunnelOptions(val id: Int) : Route()

    @Serializable data object Lock : Route()

    @Serializable data object License : Route()

    @Serializable data class Config(val id: Int) : Route()

    @Serializable
    data class SplitTunnel(val id: Int) : Route() {
        companion object {
            const val KEY_ID = "id"
        }
    }

    @Serializable data class TunnelAutoTunnel(val id: Int) : Route()

    @Serializable data object Logs : Route()

    @Serializable data object Sort : Route()
}
