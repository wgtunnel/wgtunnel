package com.zaneschepke.networkmonitor

import com.zaneschepke.networkmonitor.util.WifiSecurityType

data class ConnectivityState(
    val activeNetwork: ActiveNetwork,
    val locationPermissionsGranted: Boolean,
    val locationServicesEnabled: Boolean,
) {
    fun hasInternet(): Boolean = activeNetwork !is ActiveNetwork.Disconnected

    override fun toString(): String {
        val networkInfo =
            when (activeNetwork) {
                is ActiveNetwork.Disconnected -> "Disconnected"
                is ActiveNetwork.Ethernet -> "Ethernet"
                is ActiveNetwork.Cellular -> "Cellular"
                is ActiveNetwork.Wifi -> {
                    val ssidDisplay =
                        if (activeNetwork.ssid == AndroidNetworkMonitor.ANDROID_UNKNOWN_SSID)
                            activeNetwork.ssid
                        else activeNetwork.ssid.first() + "..."
                    "Wifi(ssid=$ssidDisplay, securityType=${activeNetwork.securityType})"
                }
            }
        return "activeNetwork=$networkInfo, locationPermissionsGranted=$locationPermissionsGranted, locationServicesEnabled=$locationServicesEnabled"
    }
}

sealed class ActiveNetwork {
    data object Disconnected : ActiveNetwork()

    data object Ethernet : ActiveNetwork()

    data object Cellular : ActiveNetwork()

    data class Wifi(val ssid: String, val securityType: WifiSecurityType? = null) : ActiveNetwork()
}

data class Permissions(
    val locationServicesEnabled: Boolean = false,
    val locationPermissionGranted: Boolean = false,
)
