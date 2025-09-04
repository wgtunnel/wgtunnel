package com.zaneschepke.wireguardautotunnel.domain.model

import com.wireguard.config.Config
import com.zaneschepke.wireguardautotunnel.util.extensions.defaultName
import com.zaneschepke.wireguardautotunnel.util.extensions.isValidIpv4orIpv6Address
import com.zaneschepke.wireguardautotunnel.util.extensions.toWgQuickString
import java.io.InputStream
import java.nio.charset.StandardCharsets

data class TunnelConf(
    val id: Int = 0,
    val tunName: String,
    val wgQuick: String,
    val tunnelNetworks: Set<String> = emptySet(),
    val isMobileDataTunnel: Boolean = false,
    val isPrimaryTunnel: Boolean = false,
    val amQuick: String,
    val isActive: Boolean = false,
    val pingTarget: String? = null,
    val restartOnPingFailure: Boolean = false,
    val isEthernetTunnel: Boolean = false,
    val isIpv4Preferred: Boolean = true,
    val position: Int = 0,
) {

    val isNameKernelCompatible: Boolean = (tunName.length <= 15)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TunnelConf) return false
        return id == other.id &&
            tunName == other.tunName &&
            wgQuick == other.wgQuick &&
            amQuick == other.amQuick &&
            isPrimaryTunnel == other.isPrimaryTunnel &&
            isMobileDataTunnel == other.isMobileDataTunnel &&
            isEthernetTunnel == other.isEthernetTunnel &&
            pingTarget == other.pingTarget &&
            restartOnPingFailure == other.restartOnPingFailure &&
            tunnelNetworks == other.tunnelNetworks &&
            isIpv4Preferred == other.isIpv4Preferred
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + tunName.hashCode()
        result = 31 * result + wgQuick.hashCode()
        result = 31 * result + amQuick.hashCode()
        return result
    }

    fun isStaticallyConfigured(): Boolean {
        return toAmConfig().peers.all { it.endpoint.get().host.isValidIpv4orIpv6Address() }
    }

    fun toAmConfig(): org.amnezia.awg.config.Config {
        return configFromAmQuick(amQuick.ifBlank { wgQuick })
    }

    fun toWgConfig(): Config {
        return configFromWgQuick(wgQuick)
    }

    companion object {
        fun configFromWgQuick(wgQuick: String): Config {
            val inputStream: InputStream = wgQuick.byteInputStream()
            return inputStream.bufferedReader(StandardCharsets.UTF_8).use { Config.parse(it) }
        }

        fun configFromAmQuick(amQuick: String): org.amnezia.awg.config.Config {
            val inputStream: InputStream = amQuick.byteInputStream()
            return inputStream.bufferedReader(StandardCharsets.UTF_8).use {
                org.amnezia.awg.config.Config.parse(it)
            }
        }

        fun tunnelConfFromQuick(amQuick: String, name: String? = null): TunnelConf {
            val config = configFromAmQuick(amQuick)
            val wgQuick = config.toWgQuickString()
            return TunnelConf(
                tunName = name ?: config.defaultName(),
                wgQuick = wgQuick,
                amQuick = amQuick,
            )
        }

        private fun tunnelConfFromAmConfig(
            config: org.amnezia.awg.config.Config,
            name: String? = null,
        ): TunnelConf {
            val amQuick = config.toAwgQuickString(true, false)
            val wgQuick = config.toWgQuickString()
            return TunnelConf(
                tunName = name ?: config.defaultName(),
                wgQuick = wgQuick,
                amQuick = amQuick,
            )
        }

        private const val IPV6_ALL_NETWORKS = "::/0"
        private const val IPV4_ALL_NETWORKS = "0.0.0.0/0"
        val ALL_IPS = listOf(IPV4_ALL_NETWORKS, IPV6_ALL_NETWORKS)
        val IPV4_PUBLIC_NETWORKS =
            setOf(
                "0.0.0.0/5",
                "8.0.0.0/7",
                "11.0.0.0/8",
                "12.0.0.0/6",
                "16.0.0.0/4",
                "32.0.0.0/3",
                "64.0.0.0/2",
                "128.0.0.0/3",
                "160.0.0.0/5",
                "168.0.0.0/6",
                "172.0.0.0/12",
                "172.32.0.0/11",
                "172.64.0.0/10",
                "172.128.0.0/9",
                "173.0.0.0/8",
                "174.0.0.0/7",
                "176.0.0.0/4",
                "192.0.0.0/9",
                "192.128.0.0/11",
                "192.160.0.0/13",
                "192.169.0.0/16",
                "192.170.0.0/15",
                "192.172.0.0/14",
                "192.176.0.0/12",
                "192.192.0.0/10",
                "193.0.0.0/8",
                "194.0.0.0/7",
                "196.0.0.0/6",
                "200.0.0.0/5",
                "208.0.0.0/4",
            )
        val LAN_BYPASS_ALLOWED_IPS = setOf(IPV6_ALL_NETWORKS) + IPV4_PUBLIC_NETWORKS
    }
}
