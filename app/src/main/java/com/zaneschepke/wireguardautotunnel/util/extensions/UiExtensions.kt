package com.zaneschepke.wireguardautotunnel.util.extensions

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.navigation.NavController
import com.zaneschepke.networkmonitor.AndroidNetworkMonitor
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.navigation.isCurrentRoute

fun NavController.goFromRoot(route: Route) {
    if (currentBackStackEntry?.isCurrentRoute(route::class) == true) return
    this.navigate(route) {
        popUpTo(Route.Main) { saveState = true }
        launchSingleTop = true
    }
}

fun AndroidNetworkMonitor.WifiDetectionMethod.asTitleString(context: Context): String {
    return when (this) {
        AndroidNetworkMonitor.WifiDetectionMethod.DEFAULT -> context.getString(R.string._default)
        AndroidNetworkMonitor.WifiDetectionMethod.LEGACY -> context.getString(R.string.legacy)
        AndroidNetworkMonitor.WifiDetectionMethod.ROOT -> context.getString(R.string.root)
        AndroidNetworkMonitor.WifiDetectionMethod.SHIZUKU -> context.getString(R.string.shizuku)
    }
}

fun AndroidNetworkMonitor.WifiDetectionMethod.asDescriptionString(context: Context): String? {
    return when (this) {
        AndroidNetworkMonitor.WifiDetectionMethod.LEGACY ->
            context.getString(R.string.legacy_api_description)
        AndroidNetworkMonitor.WifiDetectionMethod.ROOT ->
            context.getString(R.string.use_root_shell_for_wifi)
        AndroidNetworkMonitor.WifiDetectionMethod.SHIZUKU ->
            context.getString(R.string.use_shell_via_shizuku)
        AndroidNetworkMonitor.WifiDetectionMethod.DEFAULT ->
            context.getString(R.string.use_android_recommended)
    }
}

fun AppMode.asTitleString(context: Context): String {
    return when (this) {
        AppMode.VPN -> context.getString(R.string.vpn)
        AppMode.PROXY ->
            context.getString(R.string.expiremental_template, context.getString(R.string.proxy))
        AppMode.KERNEL ->
            context.getString(R.string.root_required_template, context.getString(R.string.kernel))
        AppMode.LOCK_DOWN -> context.getString(R.string.expiremental_template, "Lockdown")
    }
}

@Composable
fun AppMode.asIcon(): ImageVector {
    return when (this) {
        AppMode.VPN -> Icons.Outlined.VpnKey
        AppMode.PROXY -> ImageVector.vectorResource(R.drawable.proxy)
        AppMode.KERNEL -> Icons.Outlined.Terminal
        AppMode.LOCK_DOWN -> Icons.Outlined.Lock
    }
}
