package com.zaneschepke.wireguardautotunnel.data.mapper

import com.zaneschepke.wireguardautotunnel.data.entity.TunnelConfig
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf

object TunnelConfigMapper {
    fun toTunnelConf(tunnelConfig: TunnelConfig): TunnelConf {
        return with(tunnelConfig) {
            TunnelConf(
                id,
                name,
                wgQuick,
                tunnelNetworks,
                isMobileDataTunnel,
                isPrimaryTunnel,
                amQuick,
                isActive,
                isPingEnabled,
                pingInterval,
                pingCooldown,
                pingIp,
                isEthernetTunnel,
                isIpv4Preferred,
                position,
            )
        }
    }

    fun toTunnelConfig(tunnelConf: TunnelConf): TunnelConfig {
        return with(tunnelConf) {
            TunnelConfig(
                id,
                tunName,
                wgQuick,
                tunnelNetworks.toMutableList(),
                isMobileDataTunnel,
                isPrimaryTunnel,
                amQuick,
                isActive,
                isPingEnabled,
                pingInterval,
                pingCooldown,
                pingIp,
                isEthernetTunnel,
                isIpv4Preferred,
                position,
            )
        }
    }
}
