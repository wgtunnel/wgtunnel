package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendMode
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.events.BackendCoreException
import com.zaneschepke.wireguardautotunnel.domain.events.BackendMessage
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.state.LogHealthState
import com.zaneschepke.wireguardautotunnel.domain.state.PingState
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelStatistics
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.amnezia.awg.crypto.Key
import timber.log.Timber

abstract class BaseTunnel(@ApplicationScope protected val applicationScope: CoroutineScope) :
    TunnelProvider {

    protected val errors = MutableSharedFlow<Pair<String, BackendCoreException>>()
    override val errorEvents = errors.asSharedFlow()

    private val _messageEvents = MutableSharedFlow<Pair<String, BackendMessage>>()
    override val messageEvents = _messageEvents.asSharedFlow()

    protected val activeTuns = MutableStateFlow<Map<Int, TunnelState>>(emptyMap())
    override val activeTunnels = activeTuns.asStateFlow()

    private val tunJobs = ConcurrentHashMap<Int, Job>()
    private val tunMutex = Mutex()
    private val tunStatusMutex = Mutex()

    abstract fun tunnelStateFlow(tunnelConf: TunnelConf): Flow<TunnelStatus>

    abstract override fun setBackendMode(backendMode: BackendMode)

    abstract override fun getBackendMode(): BackendMode

    abstract override fun handleDnsReresolve(tunnelConf: TunnelConf): Boolean

    abstract override fun getStatistics(tunnelId: Int): TunnelStatistics?

    override suspend fun updateTunnelStatus(
        tunnelId: Int,
        status: TunnelStatus?,
        stats: TunnelStatistics?,
        pingStates: Map<Key, PingState>?,
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

    override suspend fun startTunnel(tunnelConf: TunnelConf) {
        tunMutex.withLock {
            if (activeTuns.value.containsKey(tunnelConf.id) || tunJobs.containsKey(tunnelConf.id)) {
                return Timber.w("Tunnel is already running: ${tunnelConf.tunName}")
            }

            updateTunnelStatus(tunnelConf.id, TunnelStatus.Starting)

            val job =
                applicationScope.launch {
                    try {
                        tunnelStateFlow(tunnelConf).collect { status ->
                            updateTunnelStatus(tunnelConf.id, status)
                        }
                    } catch (e: BackendCoreException) {
                        errors.emit(tunnelConf.tunName to e)
                        updateTunnelStatus(tunnelConf.id, TunnelStatus.Down)
                    } catch (_: CancellationException) {}
                }
            tunJobs[tunnelConf.id] = job
            job.invokeOnCompletion {
                tunJobs.remove(tunnelConf.id)
                activeTuns.update { it - tunnelConf.id }
            }
        }
    }

    override suspend fun stopTunnel(tunnelId: Int) {
        tunMutex.withLock {
            updateTunnelStatus(tunnelId, TunnelStatus.Stopping)
            tunJobs[tunnelId]?.cancel() // Triggers awaitClose to stop backend
        }
    }

    private fun cleanUpTunJob(tunnelId: Int) {
        Timber.d("Removing job for $tunnelId")
        tunJobs -= tunnelId
    }
}
