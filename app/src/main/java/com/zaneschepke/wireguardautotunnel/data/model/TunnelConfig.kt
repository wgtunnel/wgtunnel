package com.zaneschepke.wireguardautotunnel.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf

@Entity(indices = [Index(value = ["name"], unique = true)])
data class TunnelConfig(
	@PrimaryKey(autoGenerate = true) val id: Int = 0,
	@ColumnInfo(name = "name") val name: String,
	@ColumnInfo(name = "wg_quick") val wgQuick: String,
	@ColumnInfo(
		name = "tunnel_networks",
		defaultValue = "",
	)
	val tunnelNetworks: MutableList<String> = mutableListOf(),
	@ColumnInfo(
		name = "is_mobile_data_tunnel",
		defaultValue = "false",
	)
	val isMobileDataTunnel: Boolean = false,
	@ColumnInfo(
		name = "is_primary_tunnel",
		defaultValue = "false",
	)
	val isPrimaryTunnel: Boolean = false,
	@ColumnInfo(
		name = "am_quick",
		defaultValue = "",
	)
	val amQuick: String = AM_QUICK_DEFAULT,
	@ColumnInfo(
		name = "is_Active",
		defaultValue = "false",
	)
	val isActive: Boolean = false,
	@ColumnInfo(
		name = "is_ping_enabled",
		defaultValue = "false",
	)
	val isPingEnabled: Boolean = false,
	@ColumnInfo(
		name = "ping_interval",
		defaultValue = "null",
	)
	val pingInterval: Long? = null,
	@ColumnInfo(
		name = "ping_cooldown",
		defaultValue = "null",
	)
	val pingCooldown: Long? = null,
	@ColumnInfo(
		name = "ping_ip",
		defaultValue = "null",
	)
	var pingIp: String? = null,
	@ColumnInfo(
		name = "is_ethernet_tunnel",
		defaultValue = "false",
	)
	var isEthernetTunnel: Boolean = false,
	@ColumnInfo(
		name = "is_ipv4_preferred",
		defaultValue = "true",
	)
	var isIpv4Preferred: Boolean = true,
) {

	fun toTunnel(): TunnelConf {
		return TunnelConf(
			id, name, wgQuick, tunnelNetworks, isMobileDataTunnel,
			isPrimaryTunnel, amQuick, isActive, isPingEnabled, pingInterval,
			pingCooldown, pingIp, isEthernetTunnel, isIpv4Preferred,
		)
	}

	companion object {

		const val AM_QUICK_DEFAULT = ""

		fun from(tunnelConf: TunnelConf): TunnelConfig {
			return with(tunnelConf) {
				return TunnelConfig(
					id, tunName, wgQuick, tunnelNetworks.toMutableList(), isMobileDataTunnel,
					isPrimaryTunnel, amQuick, isActive, isPingEnabled, pingInterval,
					pingCooldown, pingIp, isEthernetTunnel, isIpv4Preferred,
				)
			}
		}
	}
}
