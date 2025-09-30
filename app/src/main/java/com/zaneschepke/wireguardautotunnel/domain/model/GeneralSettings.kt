package com.zaneschepke.wireguardautotunnel.domain.model

import com.zaneschepke.wireguardautotunnel.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.data.model.DnsProtocol
import com.zaneschepke.wireguardautotunnel.data.model.WifiDetectionMethod

data class GeneralSettings(
    val id: Int = 0,
    val isAutoTunnelEnabled: Boolean = false,
    val isTunnelOnMobileDataEnabled: Boolean = false,
    val trustedNetworkSSIDs: Set<String> = emptySet(),
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
    val wifiDetectionMethod: WifiDetectionMethod = WifiDetectionMethod.DEFAULT,
    val tunnelPingIntervalSeconds: Int = PING_INTERVAL_DEFAULT,
    val tunnelPingAttempts: Int = PING_ATTEMPTS_DEFAULT,
    val tunnelPingTimeoutSeconds: Int? = null,
    val appMode: AppMode = AppMode.VPN,
    val dnsProtocol: DnsProtocol = DnsProtocol.SYSTEM,
    val dnsEndpoint: String? = null,
    val isTunnelGlobalsEnabled: Boolean = false,
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

    companion object {
        const val PING_INTERVAL_DEFAULT = 30
        const val PING_ATTEMPTS_DEFAULT = 3
    }
}
