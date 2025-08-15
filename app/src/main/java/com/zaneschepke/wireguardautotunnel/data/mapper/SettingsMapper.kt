package com.zaneschepke.wireguardautotunnel.data.mapper

import com.zaneschepke.networkmonitor.AndroidNetworkMonitor
import com.zaneschepke.wireguardautotunnel.data.entity.Settings
import com.zaneschepke.wireguardautotunnel.domain.model.AppSettings

object SettingsMapper {
    fun toAppSettings(settings: Settings): AppSettings {
        return AppSettings(
            id = settings.id,
            isAutoTunnelEnabled = settings.isAutoTunnelEnabled,
            isTunnelOnMobileDataEnabled = settings.isTunnelOnMobileDataEnabled,
            trustedNetworkSSIDs = settings.trustedNetworkSSIDs,
            isAlwaysOnVpnEnabled = settings.isAlwaysOnVpnEnabled,
            isTunnelOnEthernetEnabled = settings.isTunnelOnEthernetEnabled,
            isShortcutsEnabled = settings.isShortcutsEnabled,
            isTunnelOnWifiEnabled = settings.isTunnelOnWifiEnabled,
            isRestoreOnBootEnabled = settings.isRestoreOnBootEnabled,
            isMultiTunnelEnabled = settings.isMultiTunnelEnabled,
            isPingEnabled = settings.isPingEnabled,
            isWildcardsEnabled = settings.isWildcardsEnabled,
            isStopOnNoInternetEnabled = settings.isStopOnNoInternetEnabled,
            isVpnKillSwitchEnabled = settings.isVpnKillSwitchEnabled,
            isKernelKillSwitchEnabled = settings.isKernelKillSwitchEnabled,
            isLanOnKillSwitchEnabled = settings.isLanOnKillSwitchEnabled,
            debounceDelaySeconds = settings.debounceDelaySeconds,
            isDisableKillSwitchOnTrustedEnabled = settings.isDisableKillSwitchOnTrustedEnabled,
            isTunnelOnUnsecureEnabled = settings.isTunnelOnUnsecureEnabled,
            wifiDetectionMethod =
                AndroidNetworkMonitor.WifiDetectionMethod.fromValue(
                    settings.wifiDetectionMethod.value
                ),
            tunnelPingIntervalSeconds = settings.tunnelPingIntervalSeconds,
            tunnelPingAttempts = settings.tunnelPingAttempts,
            tunnelPingTimeoutSeconds = settings.tunnelPingTimeoutSeconds,
            backendMode = settings.backendMode,
            socks5ProxyEnabled = settings.socks5ProxyEnabled,
            socks5ProxyBindAddress = settings.socks5ProxyBindAddress,
            httpProxyEnabled = settings.httpProxyEnabled,
            httpProxyBindAddress = settings.httpProxyBindAddress,
            proxyUsername = settings.proxyUsername,
            proxyPassword = settings.proxyPassword
        )
    }

    fun toSettings(appSettings: AppSettings): Settings {
        return Settings(
            id = appSettings.id,
            isAutoTunnelEnabled = appSettings.isAutoTunnelEnabled,
            isTunnelOnMobileDataEnabled = appSettings.isTunnelOnMobileDataEnabled,
            trustedNetworkSSIDs = appSettings.trustedNetworkSSIDs,
            isAlwaysOnVpnEnabled = appSettings.isAlwaysOnVpnEnabled,
            isTunnelOnEthernetEnabled = appSettings.isTunnelOnEthernetEnabled,
            isShortcutsEnabled = appSettings.isShortcutsEnabled,
            isTunnelOnWifiEnabled = appSettings.isTunnelOnWifiEnabled,
            isRestoreOnBootEnabled = appSettings.isRestoreOnBootEnabled,
            isMultiTunnelEnabled = appSettings.isMultiTunnelEnabled,
            isPingEnabled = appSettings.isPingEnabled,
            isWildcardsEnabled = appSettings.isWildcardsEnabled,
            isStopOnNoInternetEnabled = appSettings.isStopOnNoInternetEnabled,
            isVpnKillSwitchEnabled = appSettings.isVpnKillSwitchEnabled,
            isKernelKillSwitchEnabled = appSettings.isKernelKillSwitchEnabled,
            isLanOnKillSwitchEnabled = appSettings.isLanOnKillSwitchEnabled,
            debounceDelaySeconds = appSettings.debounceDelaySeconds,
            isDisableKillSwitchOnTrustedEnabled = appSettings.isDisableKillSwitchOnTrustedEnabled,
            isTunnelOnUnsecureEnabled = appSettings.isTunnelOnUnsecureEnabled,
            wifiDetectionMethod =
                Settings.WifiDetectionMethod.fromValue(appSettings.wifiDetectionMethod.value),
            tunnelPingIntervalSeconds = appSettings.tunnelPingIntervalSeconds,
            tunnelPingAttempts = appSettings.tunnelPingAttempts,
            tunnelPingTimeoutSeconds = appSettings.tunnelPingTimeoutSeconds,
            backendMode = appSettings.backendMode,
            socks5ProxyEnabled = appSettings.socks5ProxyEnabled,
            socks5ProxyBindAddress = appSettings.socks5ProxyBindAddress,
            httpProxyEnabled = appSettings.httpProxyEnabled,
            httpProxyBindAddress = appSettings.httpProxyBindAddress,
            proxyUsername = appSettings.proxyUsername,
            proxyPassword = appSettings.proxyPassword
        )
    }
}
