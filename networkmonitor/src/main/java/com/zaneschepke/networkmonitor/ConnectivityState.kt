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
)

data class Permissions(
    val locationServicesEnabled: Boolean = false,
    val locationPermissionGranted: Boolean = false,
)
