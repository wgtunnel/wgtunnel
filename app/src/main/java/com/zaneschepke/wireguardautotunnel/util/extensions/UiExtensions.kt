package com.zaneschepke.wireguardautotunnel.util.extensions

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import com.zaneschepke.networkmonitor.AndroidNetworkMonitor
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.data.model.WifiDetectionMethod
import com.zaneschepke.wireguardautotunnel.ui.navigation.Route
import kotlin.reflect.KClass

@SuppressLint("RestrictedApi")
fun <T : Route> NavBackStackEntry?.isCurrentRoute(cls: KClass<T>): Boolean {
    return this?.destination?.hierarchy?.any { it.hasRoute(route = cls) } == true
}

@Composable
inline fun <reified T : ViewModel> NavBackStackEntry.sharedViewModel(
    navController: NavController
): T {
    val navGraphRoute = destination.parent?.route ?: return hiltViewModel<T>()
    val parentEntry = remember(this) { navController.getBackStackEntry(navGraphRoute) }
    return hiltViewModel(parentEntry)
}

fun WifiDetectionMethod.asTitleString(context: Context): String {
    return when (this) {
        WifiDetectionMethod.DEFAULT -> context.getString(R.string._default)
        WifiDetectionMethod.LEGACY -> context.getString(R.string.legacy)
        WifiDetectionMethod.ROOT -> context.getString(R.string.root)
        WifiDetectionMethod.SHIZUKU -> context.getString(R.string.shizuku)
    }
}

fun WifiDetectionMethod.to(): AndroidNetworkMonitor.WifiDetectionMethod {
    return when (this) {
        WifiDetectionMethod.DEFAULT -> AndroidNetworkMonitor.WifiDetectionMethod.DEFAULT
        WifiDetectionMethod.LEGACY -> AndroidNetworkMonitor.WifiDetectionMethod.LEGACY
        WifiDetectionMethod.ROOT -> AndroidNetworkMonitor.WifiDetectionMethod.ROOT
        WifiDetectionMethod.SHIZUKU -> AndroidNetworkMonitor.WifiDetectionMethod.SHIZUKU
    }
}

fun WifiDetectionMethod.asDescriptionString(context: Context): String? {
    return when (this) {
        WifiDetectionMethod.LEGACY -> context.getString(R.string.legacy_api_description)
        WifiDetectionMethod.ROOT -> context.getString(R.string.use_root_shell_for_wifi)
        WifiDetectionMethod.SHIZUKU -> context.getString(R.string.use_shell_via_shizuku)
        WifiDetectionMethod.DEFAULT -> context.getString(R.string.use_android_recommended)
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
