package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.BackendException
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.di.Kernel
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendMode
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.events.BackendCoreException
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelStatistics
import com.zaneschepke.wireguardautotunnel.domain.state.WireGuardStatistics
import com.zaneschepke.wireguardautotunnel.util.extensions.asTunnelState
import com.zaneschepke.wireguardautotunnel.util.extensions.toBackendCoreException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import com.wireguard.android.backend.Tunnel as WgTunnel

class KernelTunnel
@Inject
constructor(
    @ApplicationScope applicationScope: CoroutineScope,
    serviceManager: ServiceManager,
    appDataRepository: AppDataRepository,
    @Kernel private val backend: Backend,
) : BaseTunnel(applicationScope, appDataRepository, serviceManager) {

    private val runtimeTunnels = ConcurrentHashMap<Int, WgTunnel>()

    // TODO Add DNS settings
    override fun tunnelStateFlow(tunnelConf: TunnelConf): Flow<TunnelStatus> = callbackFlow {
        if (!tunnelConf.isNameKernelCompatible) close(BackendCoreException.TunnelNameTooLong)

        val stateChannel = Channel<WgTunnel.State>()

        val runtimeTunnel = RuntimeWgTunnel(tunnelConf, stateChannel)
        runtimeTunnels[tunnelConf.id] = runtimeTunnel

        val consumerJob = launch {
            stateChannel.consumeAsFlow().collect { state -> trySend(state.asTunnelState()) }
        }

        try {
            updateTunnelStatus(tunnelConf, TunnelStatus.Starting)
            backend.setState(runtimeTunnel, WgTunnel.State.UP, tunnelConf.toWgConfig())
        } catch (e: BackendException) {
            close(e.toBackendCoreException())
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Invalid backend arguments")
            close(BackendCoreException.Config)
        } catch (e: Exception) {
            Timber.e(e, "Error while setting tunnel state")
            close(BackendCoreException.Unknown)
        }

        awaitClose {
            try {
                backend.setState(runtimeTunnel, WgTunnel.State.DOWN, null)
            } catch (e: BackendException) {
                errors.tryEmit(tunnelConf to e.toBackendCoreException())
            } finally {
                consumerJob.cancel()
                stateChannel.close()
                runtimeTunnels.remove(tunnelConf.id)
                trySend(TunnelStatus.Down)
                close()
            }
        }
    }

    override fun getStatistics(tunnelConf: TunnelConf): TunnelStatistics? {
        return try {
            val runtimeTunnel = runtimeTunnels[tunnelConf.id] ?: return null
            WireGuardStatistics(backend.getStatistics(runtimeTunnel))
        } catch (e: Exception) {
            Timber.e(e, "Failed to get stats for ${tunnelConf.tunName}")
            null
        }
    }

    override fun setBackendMode(backendMode: BackendMode) {
        Timber.w("Not yet implemented for kernel")
    }

    override fun getBackendMode(): BackendMode {
        return BackendMode.Inactive
    }

    override suspend fun runningTunnelNames(): Set<String> {
        return super.runningTunnelNames() + backend.runningTunnelNames
    }
}
