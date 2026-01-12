package com.zaneschepke.wireguardautotunnel.core.service.autotunnel.handler

import android.content.Context
import android.os.PowerManager
import com.zaneschepke.networkmonitor.NetworkMonitor
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.domain.repository.AutoTunnelSettingsRepository
import com.zaneschepke.wireguardautotunnel.domain.state.ActiveNetwork
import com.zaneschepke.wireguardautotunnel.domain.state.AutoTunnelState
import com.zaneschepke.wireguardautotunnel.domain.state.toDomain
import com.zaneschepke.wireguardautotunnel.util.extensions.toMillis
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.amnezia.awg.config.Config
import timber.log.Timber

class AutoTunnelRoamingHandler(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher,
    private val tunnelManager: TunnelManager,
    private val networkMonitor: NetworkMonitor,
    private val settingsRepository: AutoTunnelSettingsRepository,
) {
    private var roamingJob: Job? = null
    private var roamingProcedureJob: Job? = null
    private var lastBssid: String? = null
    private val lastRoamingTriggerTime = AtomicLong(0L)

    // Flag to let the Service know we are working
    private val _isRoamingActive = AtomicBoolean(false)
    val isRoamingActive: Boolean
        get() = _isRoamingActive.get()

    // Context tracking for safety checks
    private data class RoamingContext(
        val ssid: String,
        val startBssid: String,
        val targetBssid: String,
        val startTime: Long,
    )
    private var currentRoamingContext: RoamingContext? = null

    private val wakeLock: PowerManager.WakeLock by lazy {
        (context.getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WG:RoamingLock",
        )
    }

    fun start(scope: CoroutineScope, stateFlow: StateFlow<AutoTunnelState>) {
        roamingJob?.cancel()
        roamingJob =
            scope.launch(ioDispatcher) {
                networkMonitor.connectivityStateFlow
                    .map { it.toDomain().activeNetwork }
                    .distinctUntilChanged()
                    .collect { activeNetwork ->
                        handleNetworkChange(activeNetwork, stateFlow.value)
                    }
            }
    }

    fun stop() {
        roamingJob?.cancel()
        roamingProcedureJob?.cancel()
        if (_isRoamingActive.get()) {
            _isRoamingActive.set(false)
            currentRoamingContext = null
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    private suspend fun handleNetworkChange(
        activeNetwork: ActiveNetwork,
        currentState: AutoTunnelState,
    ) {
        when (activeNetwork) {
            is ActiveNetwork.Wifi -> {
                val currentBssid = activeNetwork.bssid
                val currentSsid = activeNetwork.ssid

                // CHECK 1: If roaming active, verify we're still on the same SSID
                if (_isRoamingActive.get()) {
                    val context = currentRoamingContext
                    if (context != null) {
                        // SSID changed during roaming → ABORT
                        if (currentSsid != context.ssid) {
                            Timber.w(
                                "ROAMING: Cancelled - SSID changed (${context.ssid} → $currentSsid)"
                            )
                            cancelRoaming()
                            lastBssid = currentBssid
                            return
                        }

                        // BSSID changed again during roaming → Cancel (already in progress)
                        if (currentBssid != null && currentBssid != context.targetBssid) {
                            Timber.d("ROAMING: BSSID changed again during roaming")
                            cancelRoaming()
                            lastBssid = currentBssid
                            return
                        }
                    }
                }

                // CHECK 2: Detect new roaming
                if (currentBssid != null && lastBssid != null && currentBssid != lastBssid) {
                    val hasActiveTunnel = currentState.activeTunnels.isNotEmpty()

                    if (hasActiveTunnel) {
                        // If roaming already in progress, SKIP
                        if (_isRoamingActive.get()) {
                            Timber.d("ROAMING: Already in progress, skipping new BSSID change")
                            return
                        }

                        val now = System.currentTimeMillis()
                        val lastRoaming = lastRoamingTriggerTime.get()

                        // Get debounce delay from settings
                        val debounceMs =
                            settingsRepository.flow.first().debounceDelaySeconds.toMillis()

                        if (lastRoaming == 0L) {
                            Timber.i(
                                "ROAMING: First WiFi switch detected ($lastBssid → $currentBssid)"
                            )
                            lastRoamingTriggerTime.set(now)

                            // Create roaming context
                            currentRoamingContext =
                                RoamingContext(
                                    ssid = currentSsid,
                                    startBssid = lastBssid,
                                    targetBssid = currentBssid,
                                    startTime = now,
                                )

                            triggerRoamingProcedure(currentState)
                        } else {
                            val elapsed = now - lastRoaming
                            if (elapsed >= debounceMs) {
                                Timber.i(
                                    "ROAMING: WiFi switch detected ($lastBssid → $currentBssid)"
                                )
                                lastRoamingTriggerTime.set(now)

                                // Create roaming context
                                currentRoamingContext =
                                    RoamingContext(
                                        ssid = currentSsid,
                                        startBssid = lastBssid,
                                        targetBssid = currentBssid,
                                        startTime = now,
                                    )

                                triggerRoamingProcedure(currentState)
                            } else {
                                Timber.d("ROAMING: Ignored - debounce active")
                            }
                        }
                    }
                }
                lastBssid = currentBssid
            }
            // ANY non-WiFi network change → CANCEL roaming
            else -> {
                if (_isRoamingActive.get()) {
                    val reason =
                        when (activeNetwork) {
                            is ActiveNetwork.Cellular -> "Switched to Cellular"
                            is ActiveNetwork.Ethernet -> "Switched to Ethernet"
                            is ActiveNetwork.Disconnected -> "Network lost"
                            else -> "Network changed"
                        }
                    Timber.i("ROAMING: Cancelled - $reason")
                    cancelRoaming()
                }
                lastBssid = null
            }
        }
    }

    private fun cancelRoaming() {
        _isRoamingActive.set(false)
        roamingProcedureJob?.cancel()
        currentRoamingContext = null
        if (wakeLock.isHeld) wakeLock.release()
    }

    private suspend fun triggerRoamingProcedure(state: AutoTunnelState) =
        withContext(ioDispatcher) {
            // Atomic check to avoid double execution
            if (_isRoamingActive.getAndSet(true)) return@withContext

            roamingProcedureJob = launch {
                val startTime = System.currentTimeMillis()

                try {
                    wakeLock.acquire(10000L)

                    val activeId = state.activeTunnels.keys.firstOrNull()
                    val originalConfig =
                        activeId?.let { id -> state.tunnels.find { it.id == id } }
                            ?: return@launch

                    val amConfig = originalConfig.toAmConfig()
                    val blockConfig = Config.Builder().setInterface(amConfig.`interface`).build()

                    val blockTunnel =
                        originalConfig.copy(
                            name = "BLOCK_${originalConfig.name}",
                            amQuick = blockConfig.toAwgQuickString(true, false),
                            wgQuick = blockConfig.toWgQuickString(true),
                        )

                    // Phase 1: Block
                    Timber.i("ROAMING: Switching to block config")
                    stopTunnelAndWait()

                    // CHECK if roaming was cancelled
                    if (!_isRoamingActive.get()) {
                        Timber.w("ROAMING: Aborted after stop")
                        return@launch
                    }

                    tunnelManager.startTunnel(blockTunnel)

                    // Phase 2: Wait Network
                    // CHECK if roaming was cancelled
                    if (!_isRoamingActive.get()) {
                        Timber.w("ROAMING: Aborted before validation")
                        stopTunnelAndWait()
                        return@launch
                    }

                    waitForNetworkValidation(2000L)

                    // Phase 3: Restore
                    // FINAL CHECK if roaming was cancelled
                    if (!_isRoamingActive.get()) {
                        Timber.w("ROAMING: Aborted after validation")
                        stopTunnelAndWait()
                        return@launch
                    }

                    Timber.i("ROAMING: Restoring original config")
                    stopTunnelAndWait()
                    tunnelManager.startTunnel(originalConfig)
                } catch (e: CancellationException) {
                    Timber.w("ROAMING: Cancelled")
                    // Cleanup on cancellation
                    try {
                        stopTunnelAndWait()
                    } catch (cleanupError: Exception) {
                        Timber.e(cleanupError, "ROAMING: Cleanup error")
                    }
                    throw e // Re-throw to propagate cancellation
                } catch (e: Exception) {
                    Timber.e(e, "ROAMING: Failed")
                } finally {
                    _isRoamingActive.set(false)
                    currentRoamingContext = null
                    if (wakeLock.isHeld) wakeLock.release()
                    val duration = System.currentTimeMillis() - startTime
                    Timber.i("ROAMING: Completed in ${duration}ms")
                }
            }
        }

    private suspend fun stopTunnelAndWait() {
        tunnelManager.stopActiveTunnels()
        withTimeoutOrNull(2000L) { tunnelManager.activeTunnels.first { it.isEmpty() } }
            ?: Timber.w("ROAMING: Tunnel stop timeout, continuing anyway")
        delay(50)
    }

    private suspend fun waitForNetworkValidation(timeoutMs: Long) =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val cm =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE)
                        as android.net.ConnectivityManager

                if (
                    cm.getNetworkCapabilities(cm.activeNetwork)
                        ?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED) ==
                        true
                ) {
                    continuation.resume(Unit) {}
                    return@suspendCancellableCoroutine
                }

                val callback =
                    object : android.net.ConnectivityManager.NetworkCallback() {
                        override fun onCapabilitiesChanged(
                            network: android.net.Network,
                            capabilities: android.net.NetworkCapabilities,
                        ) {
                            if (
                                capabilities.hasCapability(
                                    android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED
                                )
                            ) {
                                cm.unregisterNetworkCallback(this)
                                if (continuation.isActive) continuation.resume(Unit) {}
                            }
                        }
                    }

                cm.registerDefaultNetworkCallback(callback)

                val job = launch {
                    delay(timeoutMs)
                    if (continuation.isActive) {
                        cm.unregisterNetworkCallback(callback)
                        continuation.resume(Unit) {}
                    }
                }

                continuation.invokeOnCancellation {
                    job.cancel()
                    try {
                        cm.unregisterNetworkCallback(callback)
                    } catch (_: Exception) {}
                }
            }
        }
}