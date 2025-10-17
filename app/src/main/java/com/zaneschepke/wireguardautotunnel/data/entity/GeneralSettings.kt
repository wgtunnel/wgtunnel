package com.zaneschepke.wireguardautotunnel.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zaneschepke.wireguardautotunnel.data.model.AppMode

@Entity(tableName = "general_settings")
data class GeneralSettings(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "is_shortcuts_enabled", defaultValue = "0")
    val isShortcutsEnabled: Boolean = false,
    @ColumnInfo(name = "is_restore_on_boot_enabled", defaultValue = "0")
    val isRestoreOnBootEnabled: Boolean = false,
    @ColumnInfo(name = "is_multi_tunnel_enabled", defaultValue = "0")
    val isMultiTunnelEnabled: Boolean = false,
    @ColumnInfo(name = "is_tunnel_globals_enabled", defaultValue = "0")
    val isTunnelGlobalsEnabled: Boolean = false,
    @ColumnInfo(name = "app_mode", defaultValue = "0") val appMode: AppMode = AppMode.fromValue(0),
    @ColumnInfo(name = "theme", defaultValue = "AUTOMATIC") val theme: String = "AUTOMATIC",
    @ColumnInfo(name = "locale") val locale: String? = null,
    @ColumnInfo(name = "remote_key") val remoteKey: String? = null,
    @ColumnInfo(name = "is_remote_control_enabled", defaultValue = "0")
    val isRemoteControlEnabled: Boolean = false,
    @ColumnInfo(name = "is_pin_lock_enabled", defaultValue = "0")
    val isPinLockEnabled: Boolean = false,
    @ColumnInfo(name = "is_always_on_vpn_enabled", defaultValue = "0")
    val isAlwaysOnVpnEnabled: Boolean = false,
    @ColumnInfo(name = "is_lan_on_kill_switch_enabled", defaultValue = "0")
    val isLanOnKillSwitchEnabled: Boolean = false,
    @ColumnInfo(name = "custom_split_packages", defaultValue = "{}")
    val customSplitPackages: Map<String, String> = emptyMap(),
)
