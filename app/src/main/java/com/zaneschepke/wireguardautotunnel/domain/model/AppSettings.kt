package com.zaneschepke.wireguardautotunnel.domain.model

import com.zaneschepke.networkmonitor.AndroidNetworkMonitor
import com.zaneschepke.wireguardautotunnel.data.entity.Settings.Companion.HTTP_PROXY_DEFAULT_BIND_ADDRESS
import com.zaneschepke.wireguardautotunnel.data.entity.Settings.Companion.SOCKS5_PROXY_DEFAULT_BIND_ADDRESS
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendMode

data class AppSettings(
    val id: Int = 0,
    val isAutoTunnelEnabled: Boolean = false,
    val isTunnelOnMobileDataEnabled: Boolean = false,
    val trustedNetworkSSIDs: List<String> = emptyList(),
    val isAlwaysOnVpnEnabled: Boolean = false,
    val isTunnelOnEthernetEnabled: Boolean = false,
    val isShortcutsEnabled: Boolean = false,
    val isTunnelOnWifiEnabled: Boolean = false,
    val isRestoreOnBootEnabled: Boolean = false,
    val isMultiTunnelEnabled: Boolean = false,
    val isPingEnabled: Boolean = false,
    val isWildcardsEnabled: Boolean = false,
    val isStopOnNoInternetEnabled: Boolean = false,
    val isVpnKillSwitchEnabled: Boolean = false,
    val isKernelKillSwitchEnabled: Boolean = false,
    val isLanOnKillSwitchEnabled: Boolean = false,
    val debounceDelaySeconds: Int = 3,
    val isDisableKillSwitchOnTrustedEnabled: Boolean = false,
    val isTunnelOnUnsecureEnabled: Boolean = false,
    val wifiDetectionMethod: AndroidNetworkMonitor.WifiDetectionMethod =
        AndroidNetworkMonitor.WifiDetectionMethod.DEFAULT,
    val tunnelPingIntervalSeconds: Int = 30,
    val tunnelPingAttempts: Int = 3,
    val tunnelPingTimeoutSeconds: Int? = null,
    val backendMode : BackendMode = BackendMode.USERSPACE,
    val socks5ProxyEnabled: Boolean = false,
    val socks5ProxyBindAddress: String = SOCKS5_PROXY_DEFAULT_BIND_ADDRESS,
    val httpProxyEnabled: Boolean = false,
    val httpProxyBindAddress: String = HTTP_PROXY_DEFAULT_BIND_ADDRESS,
    val proxyUsername: String? = null,
    val proxyPassword: String? = null,
) {
    fun toAutoTunnelStateString(): String {
        return """
            TunnelOnWifi: $isTunnelOnWifiEnabled
            TunnelOnMobileData: $isTunnelOnMobileDataEnabled
            TunnelOnEthernet: $isTunnelOnEthernetEnabled
            Wildcards: $isWildcardsEnabled
            StopOnNoInternet: $isStopOnNoInternetEnabled
            Trusted Networks: $trustedNetworkSSIDs
        """
            .trimIndent()
    }
}
