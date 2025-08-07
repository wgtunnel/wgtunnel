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
            isKernelEnabled = settings.isKernelEnabled,
            isRestoreOnBootEnabled = settings.isRestoreOnBootEnabled,
            isMultiTunnelEnabled = settings.isMultiTunnelEnabled,
            isPingEnabled = settings.isPingEnabled,
            isAmneziaEnabled = settings.isAmneziaEnabled,
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
            isKernelEnabled = appSettings.isKernelEnabled,
            isRestoreOnBootEnabled = appSettings.isRestoreOnBootEnabled,
            isMultiTunnelEnabled = appSettings.isMultiTunnelEnabled,
            isPingEnabled = appSettings.isPingEnabled,
            isAmneziaEnabled = appSettings.isAmneziaEnabled,
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
        )
    }
}
