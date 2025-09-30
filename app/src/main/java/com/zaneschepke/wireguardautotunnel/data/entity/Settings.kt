package com.zaneschepke.wireguardautotunnel.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zaneschepke.wireguardautotunnel.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.data.model.DnsProtocol
import com.zaneschepke.wireguardautotunnel.data.model.WifiDetectionMethod

@Entity
data class Settings(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "is_tunnel_enabled", defaultValue = "0")
    val isAutoTunnelEnabled: Boolean = false,
    @ColumnInfo(name = "is_tunnel_on_mobile_data_enabled", defaultValue = "0")
    val isTunnelOnMobileDataEnabled: Boolean = false,
    @ColumnInfo(name = "trusted_network_ssids", defaultValue = "")
    val trustedNetworkSSIDs: Set<String> = emptySet(),
    @ColumnInfo(name = "is_always_on_vpn_enabled", defaultValue = "0")
    val isAlwaysOnVpnEnabled: Boolean = false,
    @ColumnInfo(name = "is_tunnel_on_ethernet_enabled", defaultValue = "0")
    val isTunnelOnEthernetEnabled: Boolean = false,
    @ColumnInfo(name = "is_shortcuts_enabled", defaultValue = "0")
    val isShortcutsEnabled: Boolean = false,
    @ColumnInfo(name = "is_tunnel_on_wifi_enabled", defaultValue = "0")
    val isTunnelOnWifiEnabled: Boolean = false,
    @ColumnInfo(name = "is_restore_on_boot_enabled", defaultValue = "0")
    val isRestoreOnBootEnabled: Boolean = false,
    @ColumnInfo(name = "is_multi_tunnel_enabled", defaultValue = "0")
    val isMultiTunnelEnabled: Boolean = false,
    @ColumnInfo(name = "is_ping_enabled", defaultValue = "0") val isPingEnabled: Boolean = false,
    @ColumnInfo(name = "is_wildcards_enabled", defaultValue = "0")
    val isWildcardsEnabled: Boolean = false,
    @ColumnInfo(name = "is_stop_on_no_internet_enabled", defaultValue = "0")
    val isStopOnNoInternetEnabled: Boolean = false,
    @ColumnInfo(name = "is_lan_on_kill_switch_enabled", defaultValue = "0")
    val isLanOnKillSwitchEnabled: Boolean = false,
    @ColumnInfo(name = "debounce_delay_seconds", defaultValue = "3")
    val debounceDelaySeconds: Int = 3,
    @ColumnInfo(name = "is_disable_kill_switch_on_trusted_enabled", defaultValue = "0")
    val isDisableKillSwitchOnTrustedEnabled: Boolean = false,
    @ColumnInfo(name = "is_tunnel_on_unsecure_enabled", defaultValue = "0")
    val isTunnelOnUnsecureEnabled: Boolean = false,
    @ColumnInfo(name = "wifi_detection_method", defaultValue = "0")
    val wifiDetectionMethod: WifiDetectionMethod = WifiDetectionMethod.fromValue(0),
    @ColumnInfo(name = "is_ping_monitoring_enabled", defaultValue = "1")
    val isPingMonitoringEnabled: Boolean = true,
    @ColumnInfo(name = "tunnel_ping_interval_sec", defaultValue = "30")
    val tunnelPingIntervalSeconds: Int = 30,
    @ColumnInfo(name = "tunnel_ping_attempts", defaultValue = "3") val tunnelPingAttempts: Int = 3,
    @ColumnInfo(name = "tunnel_ping_timeout_sec") val tunnelPingTimeoutSeconds: Int? = null,
    @ColumnInfo(name = "app_mode", defaultValue = "0") val appMode: AppMode = AppMode.fromValue(0),
    @ColumnInfo(name = "dns_protocol", defaultValue = "0")
    val dnsProtocol: DnsProtocol = DnsProtocol.fromValue(0),
    @ColumnInfo(name = "dns_endpoint") val dnsEndpoint: String? = null,
    @ColumnInfo(name = "is_tunnel_globals_enabled", defaultValue = "0")
    val isTunnelGlobalsEnabled: Boolean = false,
)
