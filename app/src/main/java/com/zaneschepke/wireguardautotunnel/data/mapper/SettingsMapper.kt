package com.zaneschepke.wireguardautotunnel.data.mapper

import com.zaneschepke.networkmonitor.AndroidNetworkMonitor
import com.zaneschepke.wireguardautotunnel.data.entity.Settings
import com.zaneschepke.wireguardautotunnel.data.model.DnsProtocol
import com.zaneschepke.wireguardautotunnel.data.model.DnsSettings
import com.zaneschepke.wireguardautotunnel.data.model.WifiDetectionMethod
import com.zaneschepke.wireguardautotunnel.domain.model.AppSettings

fun Settings.toAppSettings(): AppSettings {
    return AppSettings(
        id = id,
        isAutoTunnelEnabled = isAutoTunnelEnabled,
        isTunnelOnMobileDataEnabled = isTunnelOnMobileDataEnabled,
        trustedNetworkSSIDs = trustedNetworkSSIDs,
        isAlwaysOnVpnEnabled = isAlwaysOnVpnEnabled,
        isTunnelOnEthernetEnabled = isTunnelOnEthernetEnabled,
        isShortcutsEnabled = isShortcutsEnabled,
        isTunnelOnWifiEnabled = isTunnelOnWifiEnabled,
        isRestoreOnBootEnabled = isRestoreOnBootEnabled,
        isMultiTunnelEnabled = isMultiTunnelEnabled,
        isPingEnabled = isPingEnabled,
        isWildcardsEnabled = isWildcardsEnabled,
        isStopOnNoInternetEnabled = isStopOnNoInternetEnabled,
        isLanOnKillSwitchEnabled = isLanOnKillSwitchEnabled,
        debounceDelaySeconds = debounceDelaySeconds,
        isDisableKillSwitchOnTrustedEnabled = isDisableKillSwitchOnTrustedEnabled,
        isTunnelOnUnsecureEnabled = isTunnelOnUnsecureEnabled,
        wifiDetectionMethod =
            AndroidNetworkMonitor.WifiDetectionMethod.fromValue(wifiDetectionMethod.value),
        tunnelPingIntervalSeconds = tunnelPingIntervalSeconds,
        tunnelPingAttempts = tunnelPingAttempts,
        tunnelPingTimeoutSeconds = tunnelPingTimeoutSeconds,
        appMode = appMode,
        dnsProtocol = dnsProtocol,
        dnsEndpoint = dnsEndpoint,
    )
}

fun AppSettings.toSettings(): Settings {
    return Settings(
        id = id,
        isAutoTunnelEnabled = isAutoTunnelEnabled,
        isTunnelOnMobileDataEnabled = isTunnelOnMobileDataEnabled,
        trustedNetworkSSIDs = trustedNetworkSSIDs,
        isAlwaysOnVpnEnabled = isAlwaysOnVpnEnabled,
        isTunnelOnEthernetEnabled = isTunnelOnEthernetEnabled,
        isShortcutsEnabled = isShortcutsEnabled,
        isTunnelOnWifiEnabled = isTunnelOnWifiEnabled,
        isRestoreOnBootEnabled = isRestoreOnBootEnabled,
        isMultiTunnelEnabled = isMultiTunnelEnabled,
        isPingEnabled = isPingEnabled,
        isWildcardsEnabled = isWildcardsEnabled,
        isStopOnNoInternetEnabled = isStopOnNoInternetEnabled,
        isLanOnKillSwitchEnabled = isLanOnKillSwitchEnabled,
        debounceDelaySeconds = debounceDelaySeconds,
        isDisableKillSwitchOnTrustedEnabled = isDisableKillSwitchOnTrustedEnabled,
        isTunnelOnUnsecureEnabled = isTunnelOnUnsecureEnabled,
        wifiDetectionMethod = WifiDetectionMethod.fromValue(wifiDetectionMethod.value),
        tunnelPingIntervalSeconds = tunnelPingIntervalSeconds,
        tunnelPingAttempts = tunnelPingAttempts,
        tunnelPingTimeoutSeconds = tunnelPingTimeoutSeconds,
        appMode = appMode,
        dnsProtocol = dnsProtocol,
        dnsEndpoint = dnsEndpoint,
    )
}

fun AppSettings.toDomain(): DnsSettings {
    return DnsSettings(
        protocol =
            DnsProtocol.entries.toTypedArray().getOrElse(dnsProtocol.value) { DnsProtocol.SYSTEM },
        endpoint = dnsEndpoint,
    )
}

fun DnsSettings.toAppSettings(existing: AppSettings): AppSettings {
    return existing.copy(dnsProtocol = protocol, dnsEndpoint = endpoint)
}
