package com.zaneschepke.wireguardautotunnel.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Route {
    @Serializable data object TunnelsGraph : Route()

    @Serializable data object AutoTunnelGraph : Route()

    @Serializable data object SettingsGraph : Route()

    @Serializable data object SupportGraph : Route()

    @Serializable data object Support : Route()

    @Serializable data object Lock : Route()

    @Serializable data object License : Route()

    @Serializable data object Logs : Route()

    @Serializable data object Appearance : Route()

    @Serializable data object Language : Route()

    @Serializable data object Display : Route()

    @Serializable data object Tunnels : Route()

    @Serializable data class TunnelOptions(val id: Int) : Route()

    @Serializable data class Config(val id: Int?) : Route()

    @Serializable data class SplitTunnel(val id: Int) : Route()

    @Serializable data class TunnelAutoTunnel(val id: Int) : Route()

    @Serializable data object Sort : Route()

    @Serializable data object Settings : Route()

    @Serializable data object TunnelMonitoring : Route()

    @Serializable data object SystemFeatures : Route()

    @Serializable data object Dns : Route()

    @Serializable data object ProxySettings : Route()

    @Serializable data object AutoTunnel : Route()

    @Serializable data object AdvancedAutoTunnel : Route()

    @Serializable data object WifiDetectionMethod : Route()

    @Serializable data object LocationDisclosure : Route()
}
