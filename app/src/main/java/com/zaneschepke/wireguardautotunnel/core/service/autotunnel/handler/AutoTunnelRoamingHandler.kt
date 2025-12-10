package com.zaneschepke.wireguardautotunnel.core.service.autotunnel.handler

import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.data.model.DnsProtocol
import com.zaneschepke.wireguardautotunnel.data.model.DnsProvider
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConfig
import kotlinx.coroutines.*
import timber.log.Timber
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject

class AutoTunnelRoamingHandler @Inject constructor(
    private val tunnelManager: TunnelManager
) {
    private var lastBssid: String? = null
    private var roamingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val STABILIZATION_DELAY = 250L 
        private const val INTERNET_CHECK_TIMEOUT = 5000L
        
        private const val MAX_RESTART_ATTEMPTS = 4
        
        // Fixed Polling constants
        private const val POLLING_INTERVAL = 250L
        // 2 endpoints * 6 cycles = 12 tests (3s Max)
        private const val ROTATION_CYCLES = 6 
        private const val MAX_POLLING_ATTEMPTS = 2 * ROTATION_CYCLES 
        
        private const val SOCKET_CLOSE_DELAY = 500L
        
        // Exponential Backoff Constants
        private const val BASE_BACKOFF_DELAY = 500L 
        private const val MAX_BACKOFF_DELAY = 8000L 

        // Rotation list (Cloudflare + AdGuard, 443 ONLY for maximum compatibility)
        private val ENDPOINTS_ROTATION = listOf(
            // 1. Cloudflare Web (443)
            DnsProvider.CLOUDFLARE.asAddress(DnsProtocol.SYSTEM) to 443,
            // 2. AdGuard Web (443)
            DnsProvider.ADGUARD.asAddress(DnsProtocol.SYSTEM) to 443
        )
    }

    // Helper function to perform a single socket check
    private suspend fun checkSocketConnectivity(host: String, port: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            withTimeout(INTERNET_CHECK_TIMEOUT) {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), 2000)
                    socket.isConnected
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun isInternetAccessible(): Boolean {
        // This function is for the initial check (before entering the main loop)
        for (endpoint in ENDPOINTS_ROTATION) {
             if (checkSocketConnectivity(endpoint.first, endpoint.second)) {
                 return true
             }
        }
        return false
    }

    fun onRoamingDetected(tunnelConfig: TunnelConfig) {
        roamingJob?.cancel()
        
        Timber.i("RoamingHandler: Handling roaming for ${tunnelConfig.name}")
        
        roamingJob = scope.launch {
            handleRoaming(tunnelConfig)
        }
    }

    private suspend fun handleRoaming(config: TunnelConfig) {
        try {
            var attempts = 0
            var success = false
            var currentBackoff = BASE_BACKOFF_DELAY
            
            delay(STABILIZATION_DELAY)

            // Initial check: if our external socket check works, we assume connection is fine.
            if (isInternetAccessible()) {
                Timber.i("RoamingHandler: Initial external check passed. No restart needed.")
                return
            }

            Timber.w("RoamingHandler: External connectivity lost. Initiating restart sequence.")

            while (attempts < MAX_RESTART_ATTEMPTS && !success) {
                attempts++
                
                // --- 1. APPLY BACKOFF BEFORE ATTEMPT 2, 3, 4 ---
                if (attempts > 1) {
                    Timber.d("RoamingHandler: Waiting ${currentBackoff}ms before restart (Attempt $attempts/$MAX_RESTART_ATTEMPTS)...")
                    delay(currentBackoff)
                }

                Timber.d("RoamingHandler: Starting tunnel restart (Attempt $attempts)...")
                
                try {
                    // 2. Hard Restart
                    tunnelManager.stopTunnel(config.id)
                    delay(SOCKET_CLOSE_DELAY) // 500ms for socket cleanup
                    tunnelManager.startTunnel(config)
                    
                    // 3. Sondage Round-Robin fixe (12 vérifications @ 250ms = 3s Max)
                    repeat(MAX_POLLING_ATTEMPTS) { index ->
                        // Rotation séquentielle des 2 endpoints
                        val endpoint = ENDPOINTS_ROTATION[index % ENDPOINTS_ROTATION.size]
                        
                        if (checkSocketConnectivity(endpoint.first, endpoint.second)) {
                            success = true
                            return@repeat // Connexion trouvée, sortie de la boucle de sondage
                        }
                        delay(POLLING_INTERVAL) // 250ms
                    }

                    if (!success) {
                        Timber.w("RoamingHandler: Attempt $attempts failed after 3s fixed polling.")
                    } else {
                        Timber.i("RoamingHandler: Connection restored successfully!")
                    }

                } catch (e: Exception) {
                    Timber.e(e, "RoamingHandler: Error during restart attempt $attempts")
                }
                
                // 4. Update Backoff for next potential attempt
                if (!success && attempts < MAX_RESTART_ATTEMPTS) {
                    currentBackoff = (currentBackoff * 2).coerceAtMost(MAX_BACKOFF_DELAY)
                }
            }
            
            if (!success) {
                Timber.e("RoamingHandler: Failed to recover connection after $MAX_RESTART_ATTEMPTS attempts.")
            }

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "RoamingHandler: Critical error")
        }
    }

    fun cleanup() {
        roamingJob?.cancel()
        scope.cancel()
    }
}
