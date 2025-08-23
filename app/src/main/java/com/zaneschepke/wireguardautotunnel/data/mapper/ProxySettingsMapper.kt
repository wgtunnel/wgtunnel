package com.zaneschepke.wireguardautotunnel.data.mapper

import com.zaneschepke.wireguardautotunnel.data.entity.ProxySettings
import com.zaneschepke.wireguardautotunnel.domain.model.AppProxySettings

object ProxySettingsMapper {
    fun to(proxySettings: ProxySettings): AppProxySettings =
        with(proxySettings) {
            AppProxySettings(
                id,
                socks5ProxyEnabled,
                socks5ProxyBindAddress,
                httpProxyEnabled,
                httpProxyBindAddress,
                proxyUsername,
                proxyPassword,
            )
        }

    fun to(proxySettings: AppProxySettings): ProxySettings =
        with(proxySettings) {
            ProxySettings(
                id,
                socks5ProxyEnabled,
                socks5ProxyBindAddress,
                httpProxyEnabled,
                httpProxyBindAddress,
                proxyUsername,
                proxyPassword,
            )
        }
}
