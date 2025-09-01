package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.data.model.DnsProtocol
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendMode
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.events.BackendCoreException
import com.zaneschepke.wireguardautotunnel.domain.model.AppProxySettings
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.domain.state.AmneziaStatistics
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelStatistics
import com.zaneschepke.wireguardautotunnel.util.extensions.asAmBackendMode
import com.zaneschepke.wireguardautotunnel.util.extensions.asBackendMode
import com.zaneschepke.wireguardautotunnel.util.extensions.toBackendCoreException
import kotlinx.coroutines.CoroutineScope
import org.amnezia.awg.backend.Backend
import org.amnezia.awg.backend.BackendException
import org.amnezia.awg.backend.ProxyGoBackend
import org.amnezia.awg.backend.Tunnel
import org.amnezia.awg.config.Config
import org.amnezia.awg.config.DnsSettings
import org.amnezia.awg.config.proxy.HttpProxy
import org.amnezia.awg.config.proxy.Proxy
import org.amnezia.awg.config.proxy.Socks5Proxy
import timber.log.Timber
import java.io.IOException
import java.util.*
import javax.inject.Inject

class UserspaceTunnel
@Inject
constructor(
    applicationScope: CoroutineScope,
    val serviceManager: ServiceManager,
    val appDataRepository: AppDataRepository,
    private val backend: Backend,
) : BaseTunnel(applicationScope, appDataRepository, serviceManager) {

    override suspend fun startBackend(tunnel: TunnelConf) {
        try {
            updateTunnelStatus(tunnel, TunnelStatus.Starting)

            val proxies: List<Proxy> =
                when (backend) {
                    is ProxyGoBackend -> {
                        val proxySettings = appDataRepository.proxySettings.get()
                        Timber.d("Adding proxy configs")
                        buildList {
                            if (proxySettings.socks5ProxyEnabled) {
                                add(
                                    Socks5Proxy(
                                        proxySettings.socks5ProxyBindAddress
                                            ?: AppProxySettings.DEFAULT_SOCKS_BIND_ADDRESS,
                                        proxySettings.proxyUsername,
                                        proxySettings.proxyPassword,
                                    )
                                )
                            }
                            if (proxySettings.httpProxyEnabled) {
                                add(
                                    HttpProxy(
                                        proxySettings.httpProxyBindAddress
                                            ?: AppProxySettings.DEFAULT_HTTP_BIND_ADDRESS,
                                        proxySettings.proxyUsername,
                                        proxySettings.proxyPassword,
                                    )
                                )
                            }
                        }
                    }
                    else -> emptyList()
                }
            val setting = appDataRepository.settings.get()
            val config = tunnel.toAmConfig()
            val updatedConfig =
                Config.Builder()
                    .apply {
                        setInterface(config.`interface`)
                        addPeers(config.peers)
                        addProxies(proxies)
                        setDnsSettings(
                            DnsSettings(
                                setting.dnsProtocol == DnsProtocol.DOH,
                                Optional.ofNullable(setting.dnsEndpoint),
                            )
                        )
                    }
                    .build()
            backend.setState(tunnel, Tunnel.State.UP, updatedConfig)
        } catch (e: BackendException) {
            Timber.e(e, "Failed to start up backend for tunnel ${tunnel.name}")
            throw e.toBackendCoreException()
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Failed to start up backend for tunnel ${tunnel.name}")
            throw BackendCoreException.Config
        }
    }

    override fun stopBackend(tunnel: TunnelConf) {
        Timber.i("Stopping tunnel ${tunnel.name} userspace")
        try {
            backend.setState(tunnel, Tunnel.State.DOWN, tunnel.toAmConfig())
        } catch (e: BackendException) {
            Timber.e(e, "Failed to stop tunnel ${tunnel.id}")
            throw e.toBackendCoreException()
        }
    }

    override fun setBackendMode(backendMode: BackendMode) {
        Timber.d("Setting backend mode: $backendMode")
        try {
            backend.backendMode = backendMode.asAmBackendMode()
        } catch (e: BackendException) {
            throw e.toBackendCoreException()
            // TODO this should be mapped to BackendException in the lib
        } catch (e: IOException) {
            throw BackendCoreException.NotAuthorized
        }
    }

    override fun getBackendMode(): BackendMode {
        return backend.backendMode.asBackendMode()
    }

    override suspend fun runningTunnelNames(): Set<String> {
        return backend.runningTunnelNames
    }

    override fun getStatistics(tunnelConf: TunnelConf): TunnelStatistics? {
        return try {
            AmneziaStatistics(backend.getStatistics(tunnelConf))
        } catch (e: Exception) {
            Timber.e(e, "Failed to get stats for ${tunnelConf.tunName}")
            null
        }
    }
}
