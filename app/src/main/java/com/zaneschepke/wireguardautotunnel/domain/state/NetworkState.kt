package com.zaneschepke.wireguardautotunnel.domain.state

import com.zaneschepke.networkmonitor.ConnectivityState
import com.zaneschepke.networkmonitor.util.WifiSecurityType

data class NetworkState(
    val isWifiConnected: Boolean = false,
    val isMobileDataConnected: Boolean = false,
    val isEthernetConnected: Boolean = false,
    val wifiName: String? = null,
    val isWifiSecure: Boolean? = null,
    val locationServicesEnabled: Boolean? = null,
    val locationPermissionGranted: Boolean? = null,
) {
    fun hasNoCapabilities(): Boolean {
        return !isWifiConnected && !isMobileDataConnected && !isEthernetConnected
    }

    companion object {
        fun from(connectivityState: ConnectivityState): NetworkState {
            return NetworkState(
                isWifiSecure =
                    when (connectivityState.wifiState.securityType) {
                        WifiSecurityType.OPEN,
                        WifiSecurityType.UNKNOWN -> false
                        null -> null
                        else -> true
                    },
                isWifiConnected = connectivityState.wifiState.connected,
                isMobileDataConnected = connectivityState.cellularConnected,
                isEthernetConnected = connectivityState.ethernetConnected,
                wifiName = connectivityState.wifiState.ssid,
                locationPermissionGranted = connectivityState.wifiState.locationPermissionsGranted,
                locationServicesEnabled = connectivityState.wifiState.locationServicesEnabled,
            )
        }
    }
}
