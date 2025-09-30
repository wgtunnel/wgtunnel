package com.zaneschepke.wireguardautotunnel.ui.navigation

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
sealed class Route {

    @Keep @Serializable data object TunnelsGraph : Route()

    @Keep @Serializable data object AutoTunnelGraph : Route()

    @Keep @Serializable data object SettingsGraph : Route()

    @Keep @Serializable data object SupportGraph : Route()

    @Keep @Serializable data object Support : Route()

    @Keep @Serializable data object Lock : Route()

    @Keep @Serializable data object License : Route()

    @Keep @Serializable data object Logs : Route()

    @Keep @Serializable data object Appearance : Route()

    @Keep @Serializable data object Language : Route()

    @Keep @Serializable data object Display : Route()

    @Keep @Serializable data object Tunnels : Route()

    @Keep @Serializable data class TunnelOptions(val id: Int) : Route()

    @Keep @Serializable data class Config(val id: Int?) : Route()

    @Keep @Serializable data class SplitTunnel(val id: Int) : Route()

    @Keep @Serializable data class ConfigGlobal(val id: Int?) : Route()

    @Keep @Serializable data class TunnelGlobals(val id: Int) : Route()

    @Keep @Serializable data class SplitTunnelGlobal(val id: Int) : Route()

    @Keep @Serializable data class TunnelAutoTunnel(val id: Int) : Route()

    @Keep @Serializable data object Sort : Route()

    @Keep @Serializable data object Settings : Route()

    @Keep @Serializable data object TunnelMonitoring : Route()

    @Keep @Serializable data object SystemFeatures : Route()

    @Keep @Serializable data object Dns : Route()

    @Keep @Serializable data object ProxySettings : Route()

    @Keep @Serializable data object AutoTunnel : Route()

    @Keep @Serializable data object AdvancedAutoTunnel : Route()

    @Keep @Serializable data object WifiDetectionMethod : Route()

    @Keep @Serializable data object LocationDisclosure : Route()

    @Keep @Serializable data object Donate : Route()

    @Keep @Serializable data object Addresses : Route()
}
