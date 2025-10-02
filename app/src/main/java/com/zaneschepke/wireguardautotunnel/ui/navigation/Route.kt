package com.zaneschepke.wireguardautotunnel.ui.navigation

import androidx.annotation.Keep
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.input.key.Key.Companion.Home
import androidx.navigation3.runtime.NavKey
import com.zaneschepke.wireguardautotunnel.R
import kotlinx.serialization.Serializable

@Keep
@Serializable
sealed class Route : NavKey {

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

enum class Tab(
    val startRoute: Route,
    val titleRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    TUNNELS(Route.Tunnels, R.string.tunnels, Icons.Rounded.Home),
    AUTOTUNNEL(Route.AutoTunnel, R.string.auto_tunnel, Icons.Rounded.Bolt),
    SETTINGS(Route.Settings, R.string.settings, Icons.Rounded.Settings),
    SUPPORT(Route.Support, R.string.support, Icons.Rounded.QuestionMark);

    companion object {
        fun fromRoute(route: Route): Tab =
            when (route) {
                is Route.Tunnels,
                Route.Sort,
                is Route.TunnelOptions,
                is Route.Config,
                is Route.SplitTunnel,
                is Route.TunnelAutoTunnel -> TUNNELS
                is Route.AutoTunnel,
                Route.AdvancedAutoTunnel,
                Route.WifiDetectionMethod,
                Route.LocationDisclosure -> AUTOTUNNEL
                is Route.Settings,
                Route.TunnelMonitoring,
                Route.SystemFeatures,
                Route.Dns,
                is Route.TunnelGlobals,
                is Route.ConfigGlobal,
                is Route.SplitTunnelGlobal,
                Route.ProxySettings,
                Route.Appearance,
                Route.Language,
                Route.Display,
                Route.Logs -> SETTINGS
                is Route.Support,
                Route.License,
                Route.Donate,
                Route.Addresses -> SUPPORT
                else -> throw IllegalArgumentException("No tab for route $route")
            }
    }
}
