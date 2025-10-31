package com.zaneschepke.networkmonitor

import com.zaneschepke.networkmonitor.util.WifiSecurityType

data class ConnectivityState(
    val wifiState: WifiState,
    val ethernetConnected: Boolean = false,
    val cellularConnected: Boolean = false,
) {
    fun hasConnectivity(): Boolean = wifiState.connected || ethernetConnected || cellularConnected
}

data class WifiState(
    val connected: Boolean = false,
    val ssid: String? = null,
    val securityType: WifiSecurityType? = null,
    val locationPermissionsGranted: Boolean,
    val locationServicesEnabled: Boolean,
) {
    override fun toString(): String =
        "connected=$connected, ssid=${if(ssid == AndroidNetworkMonitor.ANDROID_UNKNOWN_SSID || ssid == null) ssid else ssid.first() + "..."} securityType=$securityType, locationPermissionsGranted=$locationPermissionsGranted"
}

data class Permissions(
    val locationServicesEnabled: Boolean = false,
    val locationPermissionGranted: Boolean = false,
)
