// BaseTunnel.kt (updated with bounce recovery and abstracts)
package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendMode
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.events.BackendCoreException
import com.zaneschepke.wireguardautotunnel.domain.events.BackendMessage
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.domain.state.PingState
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelStatistics
import com.zaneschepke.wireguardautotunnel.ui.state.ConfigProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.amnezia.awg.crypto.Key
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

abstract class BaseTunnel(
    @ApplicationScope protected val applicationScope: CoroutineScope,
    protected val appDataRepository: AppDataRepository,
    protected val serviceManager: ServiceManager,
) : TunnelProvider {

    protected val errors = MutableSharedFlow<Pair<TunnelConf, BackendCoreException>>()
    override val errorEvents = errors.asSharedFlow()

    private val _messageEvents = MutableSharedFlow<Pair<TunnelConf, BackendMessage>>()
    override val messageEvents = _messageEvents.asSharedFlow()

    protected val activeTuns = MutableStateFlow<Map<TunnelConf, TunnelState>>(emptyMap())
    override val activeTunnels = activeTuns.asStateFlow()

    private val tunJobs = ConcurrentHashMap<Int, Job>()
    private val tunMutex = Mutex()
    private val tunStatusMutex = Mutex()
    private val bounceTunnelMutex = Mutex()

    override val bouncingTunnelIds = ConcurrentHashMap<Int, TunnelStatus.StopReason>()

    abstract fun tunnelStateFlow(tunnelConf: TunnelConf): Flow<TunnelStatus>

    abstract override fun setBackendMode(backendMode: BackendMode)

    abstract override fun getBackendMode(): BackendMode

    abstract override fun getStatistics(tunnelConf: TunnelConf): TunnelStatistics?

    override fun hasVpnPermission(): Boolean {
        return serviceManager.hasVpnPermission()
    }

    override suspend fun updateTunnelStatus(
        tunnelConf: TunnelConf,
        status: TunnelStatus?,
        stats: TunnelStatistics?,
        pingStates: Map<Key, PingState>?,
        handshakeSuccessLogs: Boolean?,
    ) {
        tunStatusMutex.withLock {
            activeTuns.update { currentTuns ->
                val existingState = currentTuns[tunnelConf] ?: TunnelState()
                val newStatus = status ?: existingState.status
                if (newStatus == TunnelStatus.Down) {
                    Timber.d("Removing tunnel ${tunnelConf.id} from activeTunnels as state is DOWN")
                    cleanUpTunJob(tunnelConf)
                    currentTuns - tunnelConf
                } else if (
                    existingState.status == newStatus &&
                        stats == null &&
                        pingStates == null &&
                        handshakeSuccessLogs == null
                ) {
                    Timber.d("Skipping redundant state update for ${tunnelConf.id}: $newStatus")
                    currentTuns
                } else {
                    val updated =
                        existingState.copy(
                            status = newStatus,
                            statistics = stats ?: existingState.statistics,
                            pingStates = pingStates ?: existingState.pingStates,
                            handshakeSuccessLogs =
                                handshakeSuccessLogs ?: existingState.handshakeSuccessLogs,
                        )
                    currentTuns + (tunnelConf to updated)
                }
            }
            handleServiceStateOnChange()
        }
    }

    private suspend fun stopActiveTunnels() {
        activeTunnels.value.forEach { (config, state) ->
            if (state.status.isUpOrStarting()) {
                stopTunnel(config)
            }
        }
    }

    override suspend fun startTunnel(tunnelConf: TunnelConf) {
        tunMutex.withLock {
            if (activeTuns.value.containsKey(tunnelConf) || tunJobs.containsKey(tunnelConf.id)) {
                return Timber.w("Tunnel is already running: ${tunnelConf.tunName}")
            }
            // TODO in the future, this should only be for VPN mode
            if (this is UserspaceTunnel) stopActiveTunnels()

            activeTuns.update { it + (tunnelConf to TunnelState(status = TunnelStatus.Starting)) }

            val job =
                applicationScope.launch {
                    try {
                        var currentConf = tunnelConf
                        val bounceReason = bouncingTunnelIds[tunnelConf.id]
                        if (bounceReason is TunnelStatus.StopReason.Ping) {
                            Timber.i(
                                "Attempting to recover bounce failure with previously resolved endpoints for ${currentConf.tunName}"
                            )
                            try {
                                val previouslyResolved = bounceReason.previouslyResolvedEndpoints
                                val configProxy = ConfigProxy.from(currentConf.toAmConfig())
                                val updatedConfigProxy =
                                    configProxy.copy(
                                        peers =
                                            configProxy.peers.map {
                                                it.copy(
                                                    endpoint =
                                                        previouslyResolved[it.publicKey]
                                                            ?: it.endpoint
                                                )
                                            }
                                    )
                                val (wg, amnezia) = updatedConfigProxy.buildConfigs()
                                currentConf =
                                    currentConf.copy(
                                        amQuick = amnezia.toAwgQuickString(true, false),
                                        wgQuick = wg.toWgQuickString(true),
                                    )
                                _messageEvents.emit(tunnelConf to BackendMessage.BounceRecovery)
                                bouncingTunnelIds.remove(currentConf.id)
                            } catch (e: Exception) {
                                Timber.e(
                                    e,
                                    "Failed to update config with resolved endpoints for ${currentConf.tunName}",
                                )
                                errors.emit(
                                    currentConf to
                                        BackendCoreException.BounceFailed(
                                            BackendCoreException.Config
                                        )
                                )
                                updateTunnelStatus(currentConf, TunnelStatus.Down)
                                return@launch
                            }
                        }
                        tunnelStateFlow(currentConf)
                            // TODO no retry for now
                            //                        .retry { e ->
                            //                        (e is BackendCoreException &&
                            // e.isTransient()).also { retry ->
                            //                            if (retry) delay(1000) // Example retry
                            // delay for transient errors
                            //                        }
                            //                    }
                            .collect { status ->
                                updateTunnelStatus(currentConf, status)
                                handleServiceStateOnChange()
                                serviceManager.updateTunnelTile()
                                if (status == TunnelStatus.Up) {
                                    saveTunnelActiveState(currentConf, true)
                                    if (bounceReason is TunnelStatus.StopReason.Ping) {
                                        _messageEvents.emit(
                                            tunnelConf to BackendMessage.BounceSuccess
                                        )
                                    }
                                }
                            }
                    } catch (e: BackendCoreException) {
                        val emitError =
                            if (bouncingTunnelIds[tunnelConf.id] is TunnelStatus.StopReason.Ping) {
                                BackendCoreException.BounceFailed(e)
                            } else e
                        errors.emit(tunnelConf to emitError)
                        updateTunnelStatus(tunnelConf, TunnelStatus.Down)
                        saveTunnelActiveState(tunnelConf, false)
                        bouncingTunnelIds.remove(tunnelConf.id)
                    } catch (e: CancellationException) {
                        // Normal cancel, handled in awaitClose
                    }
                }
            tunJobs[tunnelConf.id] = job
            job.invokeOnCompletion {
                tunJobs.remove(tunnelConf.id)
                activeTuns.update { it - tunnelConf }
                handleServiceStateOnChange()
            }
        }
    }

    override suspend fun stopTunnel(tunnelConf: TunnelConf?, reason: TunnelStatus.StopReason) {
        if (tunnelConf == null) return stopActiveTunnels()
        tunMutex.withLock {
            updateTunnelStatus(tunnelConf, TunnelStatus.Stopping(reason))
            tunJobs[tunnelConf.id]?.cancel() // Triggers awaitClose to stop backend
        }
    }

    override suspend fun bounceTunnel(tunnelConf: TunnelConf, reason: TunnelStatus.StopReason) {
        bounceTunnelMutex.withLock {
            Timber.i(
                "Bounce tunnel ${tunnelConf.tunName} for reason: $reason, current bouncing: ${bouncingTunnelIds.size}"
            )
            bouncingTunnelIds[tunnelConf.id] = reason
            stopTunnel(tunnelConf, reason)
            delay(BOUNCE_DELAY)
            startTunnel(tunnelConf)
        }
    }

    private suspend fun saveTunnelActiveState(tunnelConf: TunnelConf, active: Boolean) {
        val tunnelCopy = tunnelConf.copy(isActive = active)
        appDataRepository.tunnels.save(tunnelCopy)
    }

    // TODO
    private fun handleServiceStateOnChange() {
        if (activeTuns.value.isEmpty()) serviceManager.stopTunnelForegroundService()
        else applicationScope.launch { serviceManager.startTunnelForegroundService() }
    }

    private fun cleanUpTunJob(tunnelConf: TunnelConf) {
        Timber.d("Removing job for ${tunnelConf.tunName}")
        tunJobs -= tunnelConf.id
    }

    override suspend fun runningTunnelNames(): Set<String> =
        activeTuns.value.keys.map { it.tunName }.toSet()

    companion object {
        const val BOUNCE_DELAY = 300L
    }
}
