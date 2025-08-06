package com.zaneschepke.wireguardautotunnel.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index(value = ["name"], unique = true)])
data class TunnelConfig(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "wg_quick") val wgQuick: String,
    @ColumnInfo(name = "tunnel_networks", defaultValue = "")
    val tunnelNetworks: List<String> = listOf(),
    @ColumnInfo(name = "is_mobile_data_tunnel", defaultValue = "false")
    val isMobileDataTunnel: Boolean = false,
    @ColumnInfo(name = "is_primary_tunnel", defaultValue = "false")
    val isPrimaryTunnel: Boolean = false,
    @ColumnInfo(name = "am_quick", defaultValue = "") val amQuick: String = AM_QUICK_DEFAULT,
    @ColumnInfo(name = "is_Active", defaultValue = "false") val isActive: Boolean = false,
    @ColumnInfo(name = "restart_on_ping_failure", defaultValue = "false")
    val restartOnPingFailure: Boolean = false,
    @ColumnInfo(name = "ping_target", defaultValue = "null") var pingTarget: String? = null,
    @ColumnInfo(name = "is_ethernet_tunnel", defaultValue = "false")
    val isEthernetTunnel: Boolean = false,
    @ColumnInfo(name = "is_ipv4_preferred", defaultValue = "true")
    val isIpv4Preferred: Boolean = true,
    @ColumnInfo(name = "position", defaultValue = "0") val position: Int = 0,
    @ColumnInfo(name = "auto_tunnel_apps", defaultValue = "[]")
    val autoTunnelApps: List<String> = listOf(),
) {

    companion object {
        const val AM_QUICK_DEFAULT = ""
    }
}