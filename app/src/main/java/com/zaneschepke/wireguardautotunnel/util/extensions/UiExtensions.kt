package com.zaneschepke.wireguardautotunnel.util.extensions

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SupervisedUserCircle
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.navigation.NavController
import com.zaneschepke.networkmonitor.AndroidNetworkMonitor
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendMode
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

fun BackendMode.asTitleString(context: Context): String {
    return when (this) {
        BackendMode.USERSPACE -> context.getString(R.string.userspace)
        BackendMode.PROXIED_USERSPACE -> context.getString(R.string.proxied_userspace)
        BackendMode.KERNEL -> context.getString(R.string.kernel)
    }
}

fun BackendMode.asDescriptionString(context: Context): String {
    return when (this) {
        BackendMode.USERSPACE -> ""
        BackendMode.PROXIED_USERSPACE -> ""
        BackendMode.KERNEL -> ""
    }
}

@Composable
fun BackendMode.asIcon(): ImageVector {
    return when(this) {
        BackendMode.USERSPACE -> Icons.Outlined.SupervisedUserCircle
        BackendMode.PROXIED_USERSPACE -> ImageVector.vectorResource(R.drawable.proxy)
        BackendMode.KERNEL -> Icons.Outlined.Terminal
    }
}