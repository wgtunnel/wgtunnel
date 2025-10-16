package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.zaneschepke.wireguardautotunnel.data.model.DnsProtocol
import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendMode
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.events.BackendCoreException
import com.zaneschepke.wireguardautotunnel.domain.model.ProxySettings
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.domain.repository.DnsSettingsRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.ProxySettingsRepository
import com.zaneschepke.wireguardautotunnel.domain.state.AmneziaStatistics
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelStatistics
import com.zaneschepke.wireguardautotunnel.util.extensions.asAmBackendMode
import com.zaneschepke.wireguardautotunnel.util.extensions.asBackendMode
import com.zaneschepke.wireguardautotunnel.util.extensions.asTunnelState
import com.zaneschepke.wireguardautotunnel.util.extensions.toBackendCoreException
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.update
import org.amnezia.awg.backend.Backend
import org.amnezia.awg.backend.BackendException
import org.amnezia.awg.backend.ProxyGoBackend
import org.amnezia.awg.backend.Tunnel as AwgTunnel
import org.amnezia.awg.config.Config
import org.amnezia.awg.config.DnsSettings
import org.amnezia.awg.config.proxy.HttpProxy
import org.amnezia.awg.config.proxy.Proxy
import org.amnezia.awg.config.proxy.Socks5Proxy
import timber.log.Timber

class UserspaceTunnel
@Inject
constructor(
    @ApplicationScope applicationScope: CoroutineScope,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val proxySettingsRepository: ProxySettingsRepository,
    private val dnsSettingsRepository: DnsSettingsRepository,
    private val backend: Backend,
) : BaseTunnel(applicationScope, ioDispatcher) {

    private val runtimeTunnels = ConcurrentHashMap<Int, AwgTunnel>()

    override fun tunnelStateFlow(tunnelConfig: TunnelConfig): Flow<TunnelStatus> = callbackFlow {
        val stateChannel = Channel<AwgTunnel.State>()

        val runtimeTunnel = RuntimeAwgTunnel(tunnelConfig, stateChannel)
        runtimeTunnels[tunnelConfig.id] = runtimeTunnel

        val consumerJob = launch {
            stateChannel.consumeAsFlow().collect { awgState -> trySend(awgState.asTunnelState()) }
        }

        try {
            withTimeout(STARTUP_TIMEOUT_MS) {
                updateTunnelStatus(tunnelConfig.id, TunnelStatus.Starting)

                val proxies: List<Proxy> =
                    when (backend) {
                        is ProxyGoBackend -> {
                            val proxySettings = proxySettingsRepository.getProxySettings()
                            Timber.d("Adding proxy configs")
                            buildList {
                                if (proxySettings.socks5ProxyEnabled) {
                                    add(
                                        Socks5Proxy(
                                            proxySettings.socks5ProxyBindAddress
                                                ?: ProxySettings.DEFAULT_SOCKS_BIND_ADDRESS,
                                            proxySettings.proxyUsername,
                                            proxySettings.proxyPassword,
                                        )
                                    )
                                }
                                if (proxySettings.httpProxyEnabled) {
                                    add(
                                        HttpProxy(
                                            proxySettings.httpProxyBindAddress
                                                ?: ProxySettings.DEFAULT_HTTP_BIND_ADDRESS,
                                            proxySettings.proxyUsername,
                                            proxySettings.proxyPassword,
                                        )
                                    )
                                }
                            }
                        }
                        else -> emptyList()
                    }
                val setting = dnsSettingsRepository.getDnsSettings()
                val config = tunnelConfig.toAmConfig()
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
                backend.setState(runtimeTunnel, AwgTunnel.State.UP, updatedConfig)
            }
        } catch (e: TimeoutCancellationException) {
            Timber.e("Startup timed out for ${tunnelConfig.name} (likely DNS hang)")
            errors.emit(tunnelConfig.name to BackendCoreException.DNS)
            forceStopTunnel(tunnelConfig.id)
            close()
        } catch (e: BackendException) {
            close(e.toBackendCoreException())
        } catch (e: IllegalArgumentException) {
            close(BackendCoreException.Config)
        } catch (e: Exception) {
            Timber.e(e, "Error while setting tunnel state")
            close(BackendCoreException.Unknown)
        }

        awaitClose {
            try {
                backend.setState(runtimeTunnel, AwgTunnel.State.DOWN, null)
            } catch (e: BackendException) {
                errors.tryEmit(tunnelConfig.name to e.toBackendCoreException())
            } finally {
                consumerJob.cancel()
                stateChannel.close()
                runtimeTunnels.remove(tunnelConfig.id)
                trySend(TunnelStatus.Down)
                close()
            }
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

    override fun handleDnsReresolve(tunnelConfig: TunnelConfig): Boolean {
        val tunnel = runtimeTunnels[tunnelConfig.id] ?: throw BackendCoreException.ServiceNotRunning
        return backend.resolveDDNS(tunnelConfig.toAmConfig(), tunnel.isIpv4ResolutionPreferred)
    }

    override suspend fun runningTunnelNames(): Set<String> {
        return backend.runningTunnelNames
    }

    override fun getStatistics(tunnelId: Int): TunnelStatistics? {
        return try {
            val runtimeTunnel = runtimeTunnels[tunnelId] ?: return null
            AmneziaStatistics(backend.getStatistics(runtimeTunnel))
        } catch (e: Exception) {
            Timber.e(e, "Failed to get stats for $tunnelId")
            null
        }
    }

    override suspend fun forceStopTunnel(tunnelId: Int) {
        val runtimeTunnel = runtimeTunnels[tunnelId] ?: return
        try {
            backend.setState(runtimeTunnel, AwgTunnel.State.DOWN, null)
        } catch (e: BackendException) {
            Timber.e(e, "Force stop failed for $tunnelId")
        } finally {
            tunJobs[tunnelId]?.cancel()
            runtimeTunnels.remove(tunnelId)
            tunJobs.remove(tunnelId)
            activeTuns.update { it - tunnelId }
            updateTunnelStatus(tunnelId, TunnelStatus.Down)
        }
    }
}
