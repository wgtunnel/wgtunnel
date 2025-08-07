package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.wireguard.android.backend.Tunnel
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.events.BackendError
import com.zaneschepke.wireguardautotunnel.domain.events.BackendMessage
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.domain.state.PingState
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelStatistics
import com.zaneschepke.wireguardautotunnel.ui.state.ConfigProxy
import com.zaneschepke.wireguardautotunnel.util.extensions.asTunnelState
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
    private val applicationScope: CoroutineScope,
    private val appDataRepository: AppDataRepository,
    private val serviceManager: ServiceManager,
) : TunnelProvider {

    private val _errorEvents =
        MutableSharedFlow<Pair<TunnelConf, BackendError>>(replay = 0, extraBufferCapacity = 1)
    override val errorEvents = _errorEvents.asSharedFlow()

    private val _messageEvents =
        MutableSharedFlow<Pair<TunnelConf, BackendMessage>>(replay = 0, extraBufferCapacity = 1)
    override val messageEvents = _messageEvents.asSharedFlow()

    private val activeTuns = MutableStateFlow<Map<TunnelConf, TunnelState>>(emptyMap())
    private val tunJobs = ConcurrentHashMap<Int, Job>()
    override val activeTunnels = activeTuns.asStateFlow()

    private val tunMutex = Mutex()
    private val tunStatusMutex = Mutex()
    private val bounceTunnelMutex = Mutex()

    override val bouncingTunnelIds = ConcurrentHashMap<Int, TunnelStatus.StopReason>()

    abstract suspend fun startBackend(tunnel: TunnelConf)

    abstract fun stopBackend(tunnel: TunnelConf)

    override fun hasVpnPermission(): Boolean {
        return serviceManager.hasVpnPermission()
    }

    override suspend fun updateTunnelStatus(
        tunnelConf: TunnelConf,
        status: TunnelStatus?,
        stats: TunnelStatistics?,
        pingStates: Map<Key, PingState>?,
    ) {
        tunStatusMutex.withLock {
            activeTuns.update { currentTuns ->
                val originalConf = currentTuns.getKeyById(tunnelConf.id) ?: tunnelConf
                val existingState = currentTuns.getValueById(tunnelConf.id) ?: TunnelState()
                val newStatus = status ?: existingState.status
                if (newStatus == TunnelStatus.Down) {
                    Timber.d("Removing tunnel ${tunnelConf.id} from activeTunnels as state is DOWN")
                    cleanUpTunJob(tunnelConf)
                    currentTuns - originalConf
                } else if (existingState.status == newStatus && stats == null && pingStates == null) {
                    Timber.d("Skipping redundant state update for ${tunnelConf.id}: $newStatus")
                    currentTuns
                } else {
                    val updated =
                        existingState.copy(
                            status = newStatus,
                            statistics = stats ?: existingState.statistics,
                            pingStates = pingStates ?: existingState.pingStates,
                        )
                    currentTuns + (originalConf to updated)
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

    private fun configureTunnelCallbacks(tunnelConf: TunnelConf) {
        Timber.d("Configuring TunnelConf instance: ${tunnelConf.hashCode()}")
        tunnelConf.setStateChangeCallback { state ->
            applicationScope.launch {
                Timber.d(
                    "State change callback triggered for tunnel ${tunnelConf.id}: ${tunnelConf.tunName} with state $state at ${System.currentTimeMillis()}"
                )
                when (state) {
                    is Tunnel.State -> updateTunnelStatus(tunnelConf, state.asTunnelState())
                    is org.amnezia.awg.backend.Tunnel.State ->
                        updateTunnelStatus(tunnelConf, state.asTunnelState())
                }
                handleServiceStateOnChange()
            }
            serviceManager.updateTunnelTile()
        }
    }

    override suspend fun startTunnel(tunnelConf: TunnelConf) {
        if (activeTuns.exists(tunnelConf.id) || tunJobs.containsKey(tunnelConf.id))
            return Timber.w("Tunnel is already running ${tunnelConf.name}")
        // For userspace, we need to make sure all previous tunnels are down
        if (this@BaseTunnel is UserspaceTunnel) stopActiveTunnels()
        tunMutex.withLock {
            val job = applicationScope.launch {
                try {
                    Timber.d("Starting tunnel ${tunnelConf.id}...")
                    startTunnelInner(tunnelConf)
                    Timber.d("Started complete for tunnel ${tunnelConf.name}...")
                    // catch cancellation that could occur before and during startTunnelInner and trigger at that suspend point
                } catch (e: CancellationException) {
                    Timber.w("Tunnel start has been cancelled as ${tunnelConf.name} failed to start")
                }
            }
            tunJobs[tunnelConf.id] = job
            job.invokeOnCompletion {
                tunJobs.remove(tunnelConf.id)
                Timber.d("Start job completed for tunnel ${tunnelConf.id}")
            }
        }
    }

    private suspend fun startTunnelInner(tunnelConf: TunnelConf) {
        configureTunnelCallbacks(tunnelConf)
        Timber.d("Starting backend for tunnel ${tunnelConf.id}...")

        var currentConf = tunnelConf
        var restoreAttempted = false
        var originalError: BackendError? = null

        while (true) {
            try {
                startBackend(currentConf)
                updateTunnelStatus(currentConf, TunnelStatus.Up)
                Timber.d("Started for tun ${currentConf.id}...")
                saveTunnelActiveState(currentConf, true)
                serviceManager.startTunnelForegroundService()
                if(restoreAttempted) _messageEvents.emit(tunnelConf to BackendMessage.BounceRecovery)
                if(bouncingTunnelIds[currentConf.id] is TunnelStatus.StopReason.Ping) {
                    _messageEvents.emit(tunnelConf to BackendMessage.BounceSuccess)
                }
                return  // Success, return
            } catch (e: BackendError) {
                originalError = originalError ?: e
                val bounceReason = bouncingTunnelIds[currentConf.id]
                if (!restoreAttempted && bounceReason is TunnelStatus.StopReason.Ping) {
                    Timber.i("Attempting to recover bounce failure with previously resolved endpoints for ${currentConf.name}")
                    try {
                        val previouslyResolved = bounceReason.previouslyResolvedEndpoints
                        val configProxy = ConfigProxy.from(currentConf.toAmConfig())
                        val updatedConfigProxy = configProxy.copy(
                            peers = configProxy.peers.map {
                                it.copy(endpoint = previouslyResolved[it.publicKey] ?: it.endpoint)
                            }
                        )
                        val (wg, amnezia) = updatedConfigProxy.buildConfigs()
                        currentConf = currentConf.copyWithCallback(
                            amQuick = amnezia.toAwgQuickString(true),
                            wgQuick = wg.toWgQuickString(true)
                        )
                        bouncingTunnelIds.remove(currentConf.id)
                        restoreAttempted = true
                        continue  // Retry
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to update config with resolved endpoints for ${currentConf.name}")
                        // Fall through to failure (will emit BounceFailed since retryAttempted=true)
                    }
                }
                Timber.e(e, "Failed to start backend for ${currentConf.name}")
                val emitError = if (restoreAttempted) BackendError.BounceFailed(originalError) else e
                _errorEvents.emit(currentConf to emitError)
                updateTunnelStatus(currentConf, TunnelStatus.Down)
                return
            }
        }
    }

    private suspend fun saveTunnelActiveState(tunnelConf: TunnelConf, active: Boolean) {
        val tunnelCopy = tunnelConf.copyWithCallback(isActive = active)
        appDataRepository.tunnels.save(tunnelCopy)
    }

    override suspend fun stopTunnel(tunnelConf: TunnelConf?, reason: TunnelStatus.StopReason) {
        if (tunnelConf == null) return stopActiveTunnels()
        tunMutex.withLock {
            if (activeTuns.isStarting(tunnelConf.id))
                return handleStuckStartingTunnelShutdown(tunnelConf)
            updateTunnelStatus(tunnelConf, TunnelStatus.Stopping(reason))
            stopTunnelInner(tunnelConf)
        }
    }

    private suspend fun stopTunnelInner(tunnelConf: TunnelConf) {
        try {
            val tunnel = activeTuns.findTunnel(tunnelConf.id) ?: return
            stopBackend(tunnel)
            saveTunnelActiveState(tunnelConf, false)
            removeActiveTunnel(tunnel)
        } catch (e: BackendError) {
            Timber.e(e, "Failed to stop tunnel ${tunnelConf.id}")
            _errorEvents.emit(tunnelConf to e)
            updateTunnelStatus(tunnelConf, TunnelStatus.Down)
        }
    }

    private fun handleServiceStateOnChange() {
        if (activeTuns.value.isEmpty() && bouncingTunnelIds.isEmpty())
            serviceManager.stopTunnelForegroundService()
    }

    private suspend fun handleStuckStartingTunnelShutdown(tunnel: TunnelConf) {
        Timber.d("Stuck in starting state so cancelling job for tunnel ${tunnel.name}")
        try {
            tunJobs[tunnel.id]?.cancel() ?: Timber.d("No job found for ${tunnel.name}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to cancel job for ${tunnel.name}")
        } finally {
            updateTunnelStatus(tunnel, TunnelStatus.Down)
        }
    }

    private fun cleanUpTunJob(tunnel: TunnelConf) {
        Timber.d("Removing job for ${tunnel.name}")
        tunJobs -= tunnel.id
    }

    private fun removeActiveTunnel(tunnelConf: TunnelConf) {
        activeTuns.update { current -> current.toMutableMap().apply { remove(tunnelConf) } }
    }

    override suspend fun bounceTunnel(tunnelConf: TunnelConf, reason: TunnelStatus.StopReason) {
        bounceTunnelMutex.withLock {
            Timber.i(
                "Bounce tunnel ${tunnelConf.name} for reason: $reason, current bouncing: ${bouncingTunnelIds.size}"
            )
            bouncingTunnelIds[tunnelConf.id] = reason
            runCatching {
                stopTunnel(tunnelConf, reason)
                delay(BOUNCE_DELAY)
                startTunnel(tunnelConf)
            }
        }
    }

    override suspend fun runningTunnelNames(): Set<String> =
        activeTuns.value.keys.map { it.tunName }.toSet()

    companion object {
        const val BOUNCE_DELAY = 300L
    }
}