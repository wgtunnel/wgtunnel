package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.BackendException
import com.wireguard.android.backend.Tunnel as WgTunnel
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.di.Kernel
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendMode
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.events.DnsFailure
import com.zaneschepke.wireguardautotunnel.domain.events.InvalidConfig
import com.zaneschepke.wireguardautotunnel.domain.events.KernelTunnelName
import com.zaneschepke.wireguardautotunnel.domain.events.UnknownError
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelStatistics
import com.zaneschepke.wireguardautotunnel.domain.state.WireGuardStatistics
import com.zaneschepke.wireguardautotunnel.util.extensions.asTunnelState
import com.zaneschepke.wireguardautotunnel.util.extensions.toBackendCoreException
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

class KernelTunnel
@Inject
constructor(
    @ApplicationScope applicationScope: CoroutineScope,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    @Kernel private val backend: Backend,
) : BaseTunnel(applicationScope, ioDispatcher) {

    private val runtimeTunnels = ConcurrentHashMap<Int, WgTunnel>()

    private fun validateWireGuardInterfaceName(name: String): Result<Unit> {
        if (name.isEmpty() || name.length > 15)
            return Result.failure(KernelTunnelName(R.string.kernel_name_error))
        if (name == "." || name == "..") {
            return Result.failure(KernelTunnelName(R.string.kernel_name_dots))
        }
        val pattern = Pattern.compile("^[a-zA-Z0-9_=+.-]{1,15}$")
        if (!pattern.matcher(name).matches()) {
            return Result.failure(KernelTunnelName(R.string.kernel_name_special_characters))
        }
        return Result.success(Unit)
    }

    // TODO Add DNS settings
    override fun tunnelStateFlow(tunnelConfig: TunnelConfig): Flow<TunnelStatus> = callbackFlow {
        validateWireGuardInterfaceName(tunnelConfig.name).onFailure { close(it) }

        val stateChannel = Channel<WgTunnel.State>()

        val runtimeTunnel = RuntimeWgTunnel(tunnelConfig, stateChannel)
        runtimeTunnels[tunnelConfig.id] = runtimeTunnel

        val consumerJob = launch {
            stateChannel.consumeAsFlow().collect { state -> trySend(state.asTunnelState()) }
        }

        try {
            withTimeout(STARTUP_TIMEOUT_MS) {
                updateTunnelStatus(tunnelConfig.id, TunnelStatus.Starting)
                backend.setState(runtimeTunnel, WgTunnel.State.UP, tunnelConfig.toWgConfig())
            }
        } catch (e: TimeoutCancellationException) {
            Timber.e("Startup timed out for ${tunnelConfig.name}")
            errors.emit(tunnelConfig.name to DnsFailure())
            forceStopTunnel(tunnelConfig.id)
            close()
        } catch (e: BackendException) {
            close(e.toBackendCoreException())
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Invalid backend arguments")
            close(InvalidConfig())
        } catch (e: Exception) {
            Timber.e(e, "Error while setting tunnel state")
            close(UnknownError())
        }

        awaitClose {
            try {
                backend.setState(runtimeTunnel, WgTunnel.State.DOWN, null)
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

    override fun getStatistics(tunnelId: Int): TunnelStatistics? {
        return try {
            val runtimeTunnel = runtimeTunnels[tunnelId] ?: return null
            WireGuardStatistics(backend.getStatistics(runtimeTunnel))
        } catch (e: Exception) {
            Timber.e(e, "Failed to get stats for $tunnelId")
            null
        }
    }

    override fun setBackendMode(backendMode: BackendMode) {
        Timber.w("Not yet implemented for kernel")
    }

    override fun getBackendMode(): BackendMode {
        return BackendMode.Inactive
    }

    override fun handleDnsReresolve(tunnelConfig: TunnelConfig): Boolean {
        throw NotImplementedError()
    }

    override suspend fun runningTunnelNames(): Set<String> {
        return backend.runningTunnelNames
    }

    override suspend fun forceStopTunnel(tunnelId: Int) {
        val runtimeTunnel = runtimeTunnels[tunnelId] ?: return
        try {
            backend.setState(runtimeTunnel, WgTunnel.State.DOWN, null)
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
