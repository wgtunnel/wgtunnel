package com.zaneschepke.networkmonitor

import android.net.Network
import android.net.NetworkCapabilities

sealed class TransportEvent {
    data class Available(
        val network: Network,
        val wifiDetectionMethod: AndroidNetworkMonitor.WifiDetectionMethod? = null,
    ) : TransportEvent()

    data class Lost(val network: Network) : TransportEvent()

    data class CapabilitiesChanged(
        val network: Network,
        val networkCapabilities: NetworkCapabilities,
        val wifiDetectionMethod: AndroidNetworkMonitor.WifiDetectionMethod? = null,
    ) : TransportEvent()

    data class LocationPermissionGranted(
        val network: Network?,
        val networkCapabilities: NetworkCapabilities?,
        val wifiDetectionMethod: AndroidNetworkMonitor.WifiDetectionMethod?,
    ) : TransportEvent()

    data class LocationServicesChanged(
        val enabled: Boolean,
        val network: Network?,
        val networkCapabilities: NetworkCapabilities?,
        val wifiDetectionMethod: AndroidNetworkMonitor.WifiDetectionMethod?,
    ) : TransportEvent()

    data object Unknown : TransportEvent()
}
