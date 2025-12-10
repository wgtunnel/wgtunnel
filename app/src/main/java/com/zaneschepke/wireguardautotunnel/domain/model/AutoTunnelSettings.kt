package com.zaneschepke.wireguardautotunnel.domain.model

import com.zaneschepke.wireguardautotunnel.data.model.WifiDetectionMethod

data class AutoTunnelSettings(
    val id: Int = 0,
    val isAutoTunnelEnabled: Boolean = false,
    val isTunnelOnMobileDataEnabled: Boolean = false,
    val trustedNetworkSSIDs: Set<String> = emptySet(),
    val isTunnelOnEthernetEnabled: Boolean = false,
    val isTunnelOnWifiEnabled: Boolean = false,
    val isWildcardsEnabled: Boolean = false, // Trusted Network Wildcards
    val isStopOnNoInternetEnabled: Boolean = false,
    val debounceDelaySeconds: Int = 3,
    val isTunnelOnUnsecureEnabled: Boolean = false,
    val wifiDetectionMethod: WifiDetectionMethod = WifiDetectionMethod.fromValue(0),
    val startOnBoot: Boolean = false,
    
    // --- ROAMING FEATURES ---
    val isBssidRoamingEnabled: Boolean = true,
    val isBssidAutoSaveEnabled: Boolean = false,
    val isBssidListEnabled: Boolean = true,
    val isBssidWildcardsEnabled: Boolean = false, // Roaming Wildcards
    val roamingSSIDs: Set<String> = emptySet()
)
