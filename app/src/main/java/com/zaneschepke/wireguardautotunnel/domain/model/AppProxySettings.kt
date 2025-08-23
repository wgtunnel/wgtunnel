package com.zaneschepke.wireguardautotunnel.domain.model

data class AppProxySettings(
    val id: Long = 0,
    val socks5ProxyEnabled: Boolean = false,
    val socks5ProxyBindAddress: String? = null,
    val httpProxyEnabled: Boolean = false,
    val httpProxyBindAddress: String? = null,
    val proxyUsername: String? = null,
    val proxyPassword: String? = null,
) {
    companion object {
        const val DEFAULT_SOCKS_BIND_ADDRESS = "127.0.0.1:25344"
        const val DEFAULT_HTTP_BIND_ADDRESS = "127.0.0.1:25345"
    }
}
