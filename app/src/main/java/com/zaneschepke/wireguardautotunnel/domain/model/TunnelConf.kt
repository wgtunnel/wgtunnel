package com.zaneschepke.wireguardautotunnel.domain.model

import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.*
import java.io.InputStream
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.withContext
import timber.log.Timber

data class TunnelConf(
    val id: Int = 0,
    val tunName: String,
    val wgQuick: String,
    val tunnelNetworks: List<String> = emptyList(),
    val isMobileDataTunnel: Boolean = false,
    val isPrimaryTunnel: Boolean = false,
    val amQuick: String,
    val isActive: Boolean = false,
    val isPingEnabled: Boolean = false,
    val pingInterval: Long? = null,
    val pingCooldown: Long? = null,
    val pingIp: String? = null,
    val isEthernetTunnel: Boolean = false,
    val isIpv4Preferred: Boolean = true,
    val position: Int = 0,
    @Transient private var stateChangeCallback: ((Any) -> Unit)? = null,
) : Tunnel, org.amnezia.awg.backend.Tunnel {

    fun setStateChangeCallback(callback: (Any) -> Unit) {
        stateChangeCallback = callback
    }

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
            isPingEnabled == other.isPingEnabled &&
            pingIp == other.pingIp &&
            pingCooldown == other.pingCooldown &&
            pingInterval == other.pingInterval &&
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

    fun copyWithCallback(
        id: Int = this.id,
        tunName: String = this.tunName,
        wgQuick: String = this.wgQuick,
        tunnelNetworks: List<String> = this.tunnelNetworks,
        isMobileDataTunnel: Boolean = this.isMobileDataTunnel,
        isPrimaryTunnel: Boolean = this.isPrimaryTunnel,
        amQuick: String = this.amQuick,
        isActive: Boolean = this.isActive,
        isPingEnabled: Boolean = this.isPingEnabled,
        pingInterval: Long? = this.pingInterval,
        pingCooldown: Long? = this.pingCooldown,
        pingIp: String? = this.pingIp,
        isEthernetTunnel: Boolean = this.isEthernetTunnel,
        isIpv4Preferred: Boolean = this.isIpv4Preferred,
    ): TunnelConf {
        return TunnelConf(
                id,
                tunName,
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
            .apply { stateChangeCallback = this@TunnelConf.stateChangeCallback }
    }

    fun toAmConfig(): org.amnezia.awg.config.Config {
        return configFromAmQuick(amQuick.ifBlank { wgQuick })
    }

    fun toWgConfig(): Config {
        return configFromWgQuick(wgQuick)
    }

    override fun getName(): String = tunName

    override fun isIpv4ResolutionPreferred(): Boolean = isIpv4Preferred

    override fun onStateChange(newState: org.amnezia.awg.backend.Tunnel.State) {
        stateChangeCallback?.invoke(newState)
    }

    override fun onStateChange(newState: Tunnel.State) {
        stateChangeCallback?.invoke(newState)
    }

    fun generateUniqueName(tunnelNames: List<String>): String {
        var tunnelName = this.tunName
        var num = 1
        while (tunnelNames.any { it == tunnelName }) {
            tunnelName =
                if (!tunnelName.hasNumberInParentheses()) {
                    "$name($num)"
                } else {
                    val pair = tunnelName.extractNameAndNumber()
                    "${pair?.first}($num)"
                }
            num++
        }
        return tunnelName
    }

    suspend fun isTunnelPingable(context: CoroutineContext): Boolean {
        return withContext(context) {
            val config = toWgConfig()
            if (pingIp != null) {
                return@withContext InetAddress.getByName(pingIp)
                    .isReachable(Constants.PING_TIMEOUT.toInt())
                    .also { Timber.i("Ping reachable $pingIp: $it") }
            }
            config.peers
                .map { peer -> peer.isReachable() }
                .all { true }
                .also { Timber.i("Ping of all peers reachable: $it") }
        }
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

        fun tunnelConfigFromAmConfig(
            config: org.amnezia.awg.config.Config,
            name: String? = null,
        ): TunnelConf {
            val amQuick = config.toAwgQuickString(true)
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
        private val IPV4_PUBLIC_NETWORKS =
            listOf(
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
        val LAN_BYPASS_ALLOWED_IPS = listOf(IPV6_ALL_NETWORKS) + IPV4_PUBLIC_NETWORKS
    }
}
