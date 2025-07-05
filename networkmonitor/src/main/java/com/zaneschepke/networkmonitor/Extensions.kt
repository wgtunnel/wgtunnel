package com.zaneschepke.networkmonitor

import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import com.wireguard.android.util.RootShell
import com.zaneschepke.networkmonitor.AndroidNetworkMonitor.Companion.ANDROID_UNKNOWN_SSID

const val WIFI_SSID_SHELL_COMMAND =
    "dumpsys wifi | grep 'Supplicant state: COMPLETED' | grep -o 'SSID: \"[^\"]*\"' | cut -d '\"' -f2"

fun RootShell.getCurrentWifiName(): String {
    val response = mutableListOf<String>()
    run(response, WIFI_SSID_SHELL_COMMAND)
    return response.firstOrNull() ?: ANDROID_UNKNOWN_SSID
}

@Suppress("DEPRECATION")
fun WifiManager.getCurrentSecurityType(): WifiSecurityType? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        WifiSecurityType.from(connectionInfo.currentSecurityType)
    } else {
        null
    }
}

fun NetworkCapabilities.getWifiSsid(): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val info: WifiInfo
        if (transportInfo is WifiInfo) {
            info = transportInfo as WifiInfo
            return info.ssid.removeSurrounding("\"").trim()
        }
    }
    return ANDROID_UNKNOWN_SSID
}
