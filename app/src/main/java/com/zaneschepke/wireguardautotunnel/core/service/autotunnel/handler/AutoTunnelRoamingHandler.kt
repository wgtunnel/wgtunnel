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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.amnezia.awg.config.Config
import timber.log.Timber

/** Performance: ~1ms leak window */
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

    private val _isRoamingActive = AtomicBoolean(false)
    val isRoamingActive: Boolean
        get() = _isRoamingActive.get()

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
        Timber.i("ROAMING: Handler starting...")
        roamingJob?.cancel()
        roamingJob =
            scope.launch(ioDispatcher) {
                try {
                    Timber.i("ROAMING: Handler started, monitoring network changes")

                    val initialNetwork =
                        networkMonitor.connectivityStateFlow.first().toDomain().activeNetwork
                    Timber.d("ROAMING: Initial network state: $initialNetwork")

                    if (initialNetwork is ActiveNetwork.Wifi) {
                        lastBssid = initialNetwork.bssid
                        Timber.d("ROAMING: Initial BSSID set to ${initialNetwork.bssid}")
                    }

                    networkMonitor.connectivityStateFlow
                        .map { it.toDomain().activeNetwork }
                        .distinctUntilChanged()
                        .collect { activeNetwork ->
                            handleNetworkChange(activeNetwork, stateFlow.value)
                        }
                } catch (e: Exception) {
                    Timber.e(e, "ROAMING: Handler startup failed")
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
                val previousBssid = lastBssid

                if (_isRoamingActive.get()) {
                    val context = currentRoamingContext
                    if (context != null) {
                        if (currentSsid != context.ssid) {
                            Timber.w(
                                "ROAMING: Cancelled - SSID changed from ${context.ssid} to $currentSsid"
                            )
                            cancelRoaming()
                            lastBssid = currentBssid
                            return
                        }
                        if (currentBssid != null && currentBssid != context.targetBssid) {
                            Timber.d(
                                "ROAMING: Cancelled - BSSID changed again (${context.targetBssid} → $currentBssid)"
                            )
                            cancelRoaming()
                            lastBssid = currentBssid
                            return
                        }
                    }
                }

                if (
                    currentBssid != null && previousBssid != null && currentBssid != previousBssid
                ) {
                    val hasActiveTunnel = currentState.activeTunnels.isNotEmpty()

                    if (hasActiveTunnel) {
                        if (_isRoamingActive.get()) {
                            Timber.d("ROAMING: Already in progress, ignoring new BSSID change")
                            return
                        }

                        val now = System.currentTimeMillis()
                        val lastRoaming = lastRoamingTriggerTime.get()
                        val debounceMs =
                            settingsRepository.flow.first().debounceDelaySeconds.toMillis()

                        if (lastRoaming == 0L || (now - lastRoaming) >= debounceMs) {
                            Timber.i(
                                "ROAMING: WiFi switch detected on $currentSsid ($previousBssid → $currentBssid)"
                            )
                            lastRoamingTriggerTime.set(now)
                            currentRoamingContext =
                                RoamingContext(currentSsid, previousBssid, currentBssid, now)
                            triggerRoamingProcedure(currentState)
                        } else {
                            val remainingMs = debounceMs - (now - lastRoaming)
                            Timber.d(
                                "ROAMING: Ignored - debounce active (${remainingMs}ms remaining)"
                            )
                        }
                    }
                }
                lastBssid = currentBssid
            }
            else -> {
                if (_isRoamingActive.get()) {
                    Timber.i("ROAMING: Cancelled - network changed to non-WiFi")
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
            if (_isRoamingActive.getAndSet(true)) return@withContext

            roamingProcedureJob = launch {
                val startTime = System.currentTimeMillis()

                try {
                    wakeLock.acquire(10000L)

                    val activeId = state.activeTunnels.keys.firstOrNull()
                    val originalConfig =
                        activeId?.let { id -> state.tunnels.find { it.id == id } }
                            ?: run {
                                Timber.e("ROAMING: No active tunnel found")
                                return@launch
                            }

                    val amConfig = originalConfig.toAmConfig()
                    val blockConfig = Config.Builder().setInterface(amConfig.`interface`).build()
                    val blockTunnel =
                        originalConfig.copy(
                            id = -1,
                            name = "BLOCK_${originalConfig.name}",
                            amQuick = blockConfig.toAwgQuickString(true, false),
                            wgQuick = blockConfig.toWgQuickString(true),
                        )

                    //
                    // PHASE 1: ATOMIC SWAP TO BLOCK
                    //

                    Timber.i("ROAMING: [1/3] Atomic swap to BLOCK config")
                    val phase1Start = System.currentTimeMillis()

                    tunnelManager.startTunnel(blockTunnel)

                    // Small delay for kernel to finalize the swap
                    delay(25)

                    // VERIFICATION: Is BLOCK actually active?
                    val blockActive =
                        withTimeoutOrNull(200L) {
                            tunnelManager.activeTunnels.first { tunnels ->
                                // Check if BLOCK (ID=-1) is active
                                tunnels.keys.contains(-1)
                            }
                            true
                        } ?: false

                    val phase1Duration = System.currentTimeMillis() - phase1Start

                    if (!blockActive) {
                        // FALLBACK: Atomic swap failed, do explicit stop
                        Timber.w(
                            "ROAMING: Atomic swap failed, using explicit stop (fallback in ${phase1Duration}ms)"
                        )
                        tunnelManager.stopActiveTunnels()
                        withTimeoutOrNull(1000L) {
                            tunnelManager.activeTunnels.first { it.isEmpty() }
                        }
                        delay(60)
                        tunnelManager.startTunnel(blockTunnel)
                        val fallbackDuration = System.currentTimeMillis() - phase1Start
                        Timber.d("ROAMING: BLOCK active via fallback (${fallbackDuration}ms total)")
                    } else {
                        Timber.d("ROAMING: BLOCK active via atomic swap in ${phase1Duration}ms ✓")
                    }

                    if (!_isRoamingActive.get()) {
                        Timber.w("ROAMING: Aborted after BLOCK swap")
                        stopTunnelAndWait()
                        return@launch
                    }

                    //
                    // PHASE 2: NETWORK VALIDATION + BSSID DETECTION
                    //
                    Timber.i("ROAMING: [2/3] Waiting for network validation + BSSID")
                    val phase2Start = System.currentTimeMillis()

                    // Step 2a: Wait for network validation (NET_CAPABILITY_VALIDATED)
                    waitForNetworkValidation(2000L)
                    val validationDuration = System.currentTimeMillis() - phase2Start
                    Timber.d("ROAMING: Network validated in ${validationDuration}ms")

                    if (!_isRoamingActive.get()) {
                        Timber.w("ROAMING: Aborted after validation")
                        stopTunnelAndWait()
                        return@launch
                    }

                    // Step 2b: Wait for BSSID detection (WiFi routes ready)
                    val targetBssid = currentRoamingContext?.targetBssid
                    if (targetBssid != null) {
                        Timber.d("ROAMING: Waiting for BSSID=$targetBssid detection")

                        val bssidDetected =
                            withTimeoutOrNull(500L) {
                                networkMonitor.connectivityStateFlow
                                    .map { it.toDomain().activeNetwork }
                                    .drop(1) // Skip current state
                                    .first { network ->
                                        network is ActiveNetwork.Wifi &&
                                            network.bssid == targetBssid
                                    }
                                true
                            } ?: false

                        if (bssidDetected) {
                            Timber.d("ROAMING: BSSID detected ✓")
                        } else {
                            Timber.w("ROAMING: BSSID not detected after 500ms (continuing anyway)")
                        }
                    }

                    // Final safety delay for route stability
                    delay(60)
                    val totalPhase2 = System.currentTimeMillis() - phase2Start
                    Timber.i("ROAMING: Network ready in ${totalPhase2}ms")

                    if (!_isRoamingActive.get()) {
                        Timber.w("ROAMING: Aborted after network check")
                        stopTunnelAndWait()
                        return@launch
                    }

                    //
                    // PHASE 3: ATOMIC SWAP TO ORIGINAL
                    //
                    Timber.i("ROAMING: [3/3] ATOMIC SWAP to original config")
                    val swapStartTime = System.currentTimeMillis()

                    var swapSuccessful = false

                    try {
                        tunnelManager.startTunnel(originalConfig)
                        delay(50)

                        val activeTunnelsMap =
                            withTimeoutOrNull(500L) { tunnelManager.activeTunnels.first() }
                                ?: emptyMap()

                        val activeTunnelIds = activeTunnelsMap.keys
                        val allTunnels = state.tunnels

                        val blockStillActive =
                            activeTunnelsMap.keys.any { id ->
                                id < 0 ||
                                    allTunnels.any { tunnel ->
                                        tunnel.id == id && tunnel.name.startsWith("BLOCK_")
                                    }
                            }

                        val originalActive = activeTunnelIds.contains(originalConfig.id)

                        if (blockStillActive || !originalActive) {
                            throw Exception(
                                "Atomic swap failed: BLOCK=$blockStillActive, Original=$originalActive"
                            )
                        }

                        swapSuccessful = true
                        val swapDuration = System.currentTimeMillis() - swapStartTime
                        Timber.i("ROAMING: ✓ Atomic swap verified in ${swapDuration}ms")
                    } catch (swapError: Exception) {
                        Timber.w(swapError, "ROAMING: Atomic swap failed, executing fallback")

                        tunnelManager.stopActiveTunnels()
                        withTimeoutOrNull(500L) {
                            tunnelManager.activeTunnels.first { it.isEmpty() }
                        }
                        delay(40)
                        tunnelManager.startTunnel(originalConfig)

                        delay(50)
                        val activeTunnelsMapAfterFallback =
                            withTimeoutOrNull(500L) { tunnelManager.activeTunnels.first() }
                                ?: emptyMap()

                        val originalActiveAfterFallback =
                            activeTunnelsMapAfterFallback.keys.contains(originalConfig.id)

                        if (originalActiveAfterFallback) {
                            swapSuccessful = true
                        }

                        val swapDuration = System.currentTimeMillis() - swapStartTime
                        Timber.i("ROAMING: ✓ Fallback completed in ${swapDuration}ms")
                    }

                    if (swapSuccessful) {
                        Timber.i("ROAMING: ✓ Successfully completed")
                    } else {
                        Timber.e("ROAMING: ✗ Completed but verification failed")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "ROAMING: ✗ Failed with exception")
                    try {
                        stopTunnelAndWait()
                    } catch (cleanupError: Exception) {
                        Timber.e(cleanupError, "ROAMING: Cleanup error")
                    }
                } finally {
                    _isRoamingActive.set(false)
                    currentRoamingContext = null
                    if (wakeLock.isHeld) wakeLock.release()
                    val duration = System.currentTimeMillis() - startTime
                    Timber.i("ROAMING: Total procedure duration: ${duration}ms")
                }
            }
        }

    private suspend fun stopTunnelAndWait() {
        tunnelManager.stopActiveTunnels()
        withTimeoutOrNull(1000L) { tunnelManager.activeTunnels.first { it.isEmpty() } }
        delay(60)
    }

    /**
     * Waits for Android network to be validated (NET_CAPABILITY_VALIDATED) Verifies that Internet
     * connectivity is actually working
     */
    private suspend fun waitForNetworkValidation(timeoutMs: Long) =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val cm =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE)
                        as android.net.ConnectivityManager

                // Quick check: already validated?
                if (
                    cm.getNetworkCapabilities(cm.activeNetwork)
                        ?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED) ==
                        true
                ) {
                    continuation.resume(Unit) {}
                    return@suspendCancellableCoroutine
                }

                // Otherwise, wait for callback
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

                // Timeout job
                val job = launch {
                    delay(timeoutMs)
                    if (continuation.isActive) {
                        cm.unregisterNetworkCallback(callback)
                        continuation.resume(Unit) {}
                    }
                }

                // Cleanup if coroutine cancelled
                continuation.invokeOnCancellation {
                    job.cancel()
                    try {
                        cm.unregisterNetworkCallback(callback)
                    } catch (_: Exception) {}
                }
            }
        }
}
