package com.zaneschepke.networkmonitor.util

import android.net.wifi.WifiInfo

enum class WifiSecurityType {
    UNKNOWN,
    OPEN,
    WEP,
    WPA2, // WPA and WPA2
    WPA3, // WPA3-Personal (SAE)
    OWE,
    WAPI, // All WAPI_PSK and WAPI_CERT
    EAP, // All EAP (covers both WPA3 and others)
    PASSPOINT, // All Passpoint versions
    DPP;

    companion object {
        fun from(securityType: Int): WifiSecurityType {
            return when (securityType) {
                WifiInfo.SECURITY_TYPE_OPEN -> OPEN
                WifiInfo.SECURITY_TYPE_WEP -> WEP
                WifiInfo.SECURITY_TYPE_PSK -> WPA2
                WifiInfo.SECURITY_TYPE_EAP -> EAP
                WifiInfo.SECURITY_TYPE_SAE -> WPA3
                WifiInfo.SECURITY_TYPE_OWE -> OWE
                WifiInfo.SECURITY_TYPE_WAPI_PSK,
                WifiInfo.SECURITY_TYPE_WAPI_CERT -> WAPI
                WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE -> EAP
                WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT -> EAP
                WifiInfo.SECURITY_TYPE_PASSPOINT_R1_R2,
                WifiInfo.SECURITY_TYPE_PASSPOINT_R3 -> PASSPOINT
                WifiInfo.SECURITY_TYPE_DPP -> DPP
                WifiInfo.SECURITY_TYPE_UNKNOWN -> UNKNOWN
                else -> UNKNOWN
            }
        }
    }
}
