package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendMode
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.events.BackendCoreException
import com.zaneschepke.wireguardautotunnel.domain.events.BackendMessage
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.domain.state.LogHealthState
import com.zaneschepke.wireguardautotunnel.domain.state.PingState
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelStatistics
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

abstract class BaseTunnel(
    @ApplicationScope protected val applicationScope: CoroutineScope,
    @IoDispatcher protected val ioDispatcher: CoroutineDispatcher,
) : TunnelProvider {

    protected val errors = MutableSharedFlow<Pair<String, BackendCoreException>>()
    override val errorEvents = errors.asSharedFlow()

    private val _messageEvents = MutableSharedFlow<Pair<String, BackendMessage>>()
    override val messageEvents = _messageEvents.asSharedFlow()

    protected val activeTuns = MutableStateFlow<Map<Int, TunnelState>>(emptyMap())
    override val activeTunnels = activeTuns.asStateFlow()

    protected val tunJobs = ConcurrentHashMap<Int, Job>()
    private val tunMutex = Mutex()
    private val tunStatusMutex = Mutex()

    abstract fun tunnelStateFlow(tunnelConfig: TunnelConfig): Flow<TunnelStatus>

    abstract override fun setBackendMode(backendMode: BackendMode)

    abstract override fun getBackendMode(): BackendMode

    abstract override suspend fun forceStopTunnel(tunnelId: Int)

    abstract override fun handleDnsReresolve(tunnelConfig: TunnelConfig): Boolean

    abstract override fun getStatistics(tunnelId: Int): TunnelStatistics?

    override suspend fun updateTunnelStatus(
        tunnelId: Int,
        status: TunnelStatus?,
        stats: TunnelStatistics?,
        pingStates: Map<String, PingState>?,
        logHealthState: LogHealthState?,
    ) {
        tunStatusMutex.withLock {
            activeTuns.update { currentTuns ->
                if (!currentTuns.containsKey(tunnelId) && status != TunnelStatus.Starting) {
                    Timber.d("Ignoring update for inactive tunnel $tunnelId")
                    return@update currentTuns
                }
                val existingState = currentTuns[tunnelId] ?: TunnelState()
                val newStatus = status ?: existingState.status
                if (newStatus == TunnelStatus.Down) {
                    Timber.d("Removing tunnel $tunnelId from activeTunnels as state is DOWN")
                    cleanUpTunJob(tunnelId)
                    currentTuns - tunnelId
                } else if (
                    existingState.status == newStatus &&
                        stats == null &&
                        pingStates == null &&
                        logHealthState == null
                ) {
                    Timber.d("Skipping redundant state update for ${tunnelId}: $newStatus")
                    currentTuns
                } else {
                    val updated =
                        existingState.copy(
                            status = newStatus,
                            statistics = stats ?: existingState.statistics,
                            pingStates = pingStates ?: existingState.pingStates,
                            logHealthState = logHealthState ?: existingState.logHealthState,
                        )
                    currentTuns + (tunnelId to updated)
                }
            }
        }
    }

    override suspend fun stopActiveTunnels() {
        activeTunnels.value.forEach { (config, state) ->
            if (state.status.isUpOrStarting()) {
                stopTunnel(config)
            }
        }
    }

    override suspend fun startTunnel(tunnelConfig: TunnelConfig) {
        tunMutex.withLock {
            if (
                activeTuns.value.containsKey(tunnelConfig.id) ||
                    tunJobs.containsKey(tunnelConfig.id)
            ) {
                return Timber.w("Tunnel is already running: ${tunnelConfig.name}")
            }

            updateTunnelStatus(tunnelConfig.id, TunnelStatus.Starting)

            val job =
                applicationScope.launch(ioDispatcher) {
                    try {
                        tunnelStateFlow(tunnelConfig).collect { status ->
                            updateTunnelStatus(tunnelConfig.id, status)
                        }
                    } catch (e: BackendCoreException) {
                        errors.emit(tunnelConfig.name to e)
                        updateTunnelStatus(tunnelConfig.id, TunnelStatus.Down)
                    } catch (_: CancellationException) {}
                }
            tunJobs[tunnelConfig.id] = job
            job.invokeOnCompletion {
                tunJobs.remove(tunnelConfig.id)
                activeTuns.update { it - tunnelConfig.id }
            }
        }
    }

    override suspend fun stopTunnel(tunnelId: Int) {
        tunMutex.withLock {
            val currentState = activeTuns.value[tunnelId]?.status ?: return@withLock
            updateTunnelStatus(tunnelId, TunnelStatus.Stopping)
            tunJobs[tunnelId]?.cancel()

            withTimeoutOrNull(STOP_TIMEOUT_MS) {
                activeTunnels.first {
                    !it.containsKey(tunnelId) || it[tunnelId]!!.status == TunnelStatus.Down
                }
            }
                ?: run {
                    Timber.w("Stop timeout for $tunnelId (was $currentState); forcing kill")
                    forceStopTunnel(tunnelId)
                }
        }
    }

    private fun cleanUpTunJob(tunnelId: Int) {
        Timber.d("Removing job for $tunnelId")
        tunJobs -= tunnelId
    }

    companion object {
        const val STARTUP_TIMEOUT_MS: Long = 15_000L
        const val STOP_TIMEOUT_MS: Long = 5_000L
    }
}
