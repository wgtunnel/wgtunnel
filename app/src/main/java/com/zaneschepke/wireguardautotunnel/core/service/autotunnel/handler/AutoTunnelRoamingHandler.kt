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
    private var lastBssid: String? = null
    private val lastRoamingTriggerTime = AtomicLong(0L)

    // Flag to let the Service know we are working
    private val _isRoamingActive = AtomicBoolean(false)
    val isRoamingActive: Boolean
        get() = _isRoamingActive.get()

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
        if (_isRoamingActive.get()) {
            _isRoamingActive.set(false)
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
                if (currentBssid != null && lastBssid != null && currentBssid != lastBssid) {
                    val hasActiveTunnel = currentState.activeTunnels.isNotEmpty()

                    if (hasActiveTunnel) {
                        val now = System.currentTimeMillis()
                        val lastRoaming = lastRoamingTriggerTime.get()

                        // Get debounce delay from settings
                        val debounceMs =
                            settingsRepository.flow.first().debounceDelaySeconds.toMillis()

                        if (lastRoaming == 0L) {
                            Timber.i(
                                "ROAMING: First WiFi switch detected ($lastBssid -> $currentBssid)"
                            )
                            lastRoamingTriggerTime.set(now)
                            triggerRoamingProcedure(currentState)
                        } else {
                            val elapsed = now - lastRoaming
                            if (elapsed >= debounceMs) {
                                Timber.i(
                                    "ROAMING: WiFi switch detected ($lastBssid -> $currentBssid)"
                                )
                                lastRoamingTriggerTime.set(now)
                                triggerRoamingProcedure(currentState)
                            } else {
                                Timber.d("ROAMING: Ignored - debounce active")
                            }
                        }
                    }
                }
                lastBssid = currentBssid
            }
            else -> {
                if (_isRoamingActive.get()) {
                    Timber.i("ROAMING: Cancelled - WiFi lost")
                    // Reset internal roaming state
                    _isRoamingActive.set(false)
                    if (wakeLock.isHeld) wakeLock.release()
                }
                lastBssid = null
            }
        }
    }

    private suspend fun triggerRoamingProcedure(state: AutoTunnelState) =
        withContext(ioDispatcher) {
            // Atomic check to avoid double execution
            if (_isRoamingActive.getAndSet(true)) return@withContext

            val startTime = System.currentTimeMillis()
            try {
                wakeLock.acquire(10000L)

                val activeId = state.activeTunnels.keys.firstOrNull()
                val originalConfig =
                    activeId?.let { id -> state.tunnels.find { it.id == id } } ?: return@withContext

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
                tunnelManager.startTunnel(blockTunnel)

                // Phase 2: Wait Network
                waitForNetworkValidation(5000L)

                // Phase 3: Restore
                Timber.i("ROAMING: Restoring original config")
                stopTunnelAndWait()
                tunnelManager.startTunnel(originalConfig)
            } catch (e: Exception) {
                Timber.e(e, "ROAMING: Failed")
            } finally {
                _isRoamingActive.set(false)
                if (wakeLock.isHeld) wakeLock.release()
                val duration = System.currentTimeMillis() - startTime
                Timber.i("ROAMING: Completed in ${duration}ms")
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
