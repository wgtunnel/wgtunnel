package com.zaneschepke.wireguardautotunnel.core.service

import android.app.Notification
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.zaneschepke.networkmonitor.NetworkMonitor
import com.zaneschepke.networkmonitor.NetworkStatus
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.core.notification.NotificationManager
import com.zaneschepke.wireguardautotunnel.core.notification.WireGuardNotification
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.enums.NotificationAction
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.distinctByKeys
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

@AndroidEntryPoint
class TunnelForegroundService : LifecycleService() {

    @Inject lateinit var notificationManager: NotificationManager

    @Inject lateinit var serviceManager: ServiceManager

    @Inject lateinit var networkMonitor: NetworkMonitor

    @Inject @IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher

    @Inject lateinit var tunnelRepo: TunnelRepository

    @Inject lateinit var tunnelManager: TunnelManager

    private val isNetworkConnected = MutableStateFlow(true)

    private val tunnelJobs = ConcurrentHashMap<TunnelConf, Job>()
    private val pingJobs = ConcurrentHashMap<TunnelConf, Job>()

    private val jobsMutex = Mutex()

    class LocalBinder(val service: TunnelForegroundService) : Binder()

    private val binder = LocalBinder(this)

    override fun onCreate() {
        super.onCreate()
        ServiceCompat.startForeground(
            this@TunnelForegroundService,
            NotificationManager.VPN_NOTIFICATION_ID,
            onCreateNotification(),
            Constants.SYSTEM_EXEMPT_SERVICE_TYPE_ID,
        )
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        ServiceCompat.startForeground(
            this@TunnelForegroundService,
            NotificationManager.VPN_NOTIFICATION_ID,
            onCreateNotification(),
            Constants.SYSTEM_EXEMPT_SERVICE_TYPE_ID,
        )
        start()
        return START_STICKY
    }

    fun start() =
        lifecycleScope.launch(ioDispatcher) {
            tunnelManager.activeTunnels.distinctByKeys().collect { activeTunnels ->
                // No active tunnels and no jobs: nothing to do
                if (activeTunnels.isEmpty() && tunnelJobs.isEmpty()) return@collect

                // Synchronize jobs with active tunnels
                synchronizeJobs(activeTunnels)
                updateServiceNotification()
            }
        }

    private suspend fun synchronizeJobs(activeTunnels: Map<TunnelConf, TunnelState>) {
        jobsMutex.withLock {
            // Stop jobs for tunnels that are no longer active
            stopInactiveJobs(activeTunnels)
            // Start jobs for new tunnels
            startNewJobs(activeTunnels)
        }
    }

    private fun stopInactiveJobs(activeTunnels: Map<TunnelConf, TunnelState>) {
        // If no active tunnels, clear all jobs
        if (activeTunnels.isEmpty()) {
            clearAllJobs()
            return
        }
        // Stop jobs for tunnels not in activeTunnels
        val tunnelsToStop = tunnelJobs.keys - activeTunnels.keys
        tunnelsToStop.forEach { tun -> stopTunnelJobs(tun) }
    }

    private fun clearAllJobs() {
        tunnelJobs.forEach { (tun, job) ->
            Timber.d("Stopping tunnel job for ${tun.tunName}")
            job.cancel()
        }
        tunnelJobs.clear()

        pingJobs.forEach { (tun, job) ->
            if (isPingBounce(tun)) {
                Timber.d("Preserving ping job for ${tun.tunName} due to PING bounce")
                return@forEach
            }
            Timber.d("Stopping ping job for ${tun.tunName}")
            job.cancel()
        }
        pingJobs.entries.removeIf { (tun, _) -> !isPingBounce(tun) }
    }

    private fun stopTunnelJobs(tun: TunnelConf) {
        tunnelJobs.remove(tun)?.cancel()
        Timber.d("Stopped tunnel job for ${tun.tunName}")
        if (isPingBounce(tun))
            return Timber.d("Preserving ${tun.tunName} ping job due to ping bounce")
        pingJobs.remove(tun)?.cancel()
        Timber.d("Stopped ping job for ${tun.tunName}")
    }

    private fun startNewJobs(activeTunnels: Map<TunnelConf, TunnelState>) {
        val tunnelsToStart = activeTunnels.keys - tunnelJobs.keys
        tunnelsToStart.forEach { tun ->
            tunnelJobs[tun] = startTunnelJobs(tun)
            Timber.d("Started tunnel job for ${tun.tunName}")

            if (pingJobs[tun]?.isActive == true) {
                Timber.d("Reusing active ping job for ${tun.tunName}")
            } else {
                pingJobs[tun]?.cancel() // Cancel any stale job
                if (tun.isPingEnabled) {
                    if (tun.isStaticallyConfigured()) {
                        Timber.d("Skipping ping for statically configured tunnel")
                    } else {
                        pingJobs[tun] = startPingJob(tun)
                        Timber.d("Started ping job for ${tun.tunName}")
                    }
                }
            }
        }
    }

    private fun isPingBounce(tun: TunnelConf): Boolean =
        tunnelManager.bouncingTunnelIds[tun.id] == TunnelStatus.StopReason.PING

    // TODO Would be cool to have this include kill switch
    // TODO also we need to include errors
    private fun updateServiceNotification() {
        val notification =
            when (tunnelJobs.size) {
                0 -> onCreateNotification()
                1 -> createTunnelNotification(tunnelJobs.keys.first())
                else -> createTunnelsNotification()
            }
        ServiceCompat.startForeground(
            this@TunnelForegroundService,
            NotificationManager.VPN_NOTIFICATION_ID,
            notification,
            Constants.SYSTEM_EXEMPT_SERVICE_TYPE_ID,
        )
    }

    // use same scope so we can cancel all of these
    private fun startTunnelJobs(tunnelConf: TunnelConf) =
        lifecycleScope.launch(ioDispatcher) {
            // monitor if we have internet connectivity
            launch { startNetworkMonitorJob() }
            // job to trigger stats emit on interval
            launch { startTunnelStatsJob(tunnelConf) }
            // monitor changes to the tunnel config
            launch { startTunnelConfChangesJob(tunnelConf) }
        }

    private suspend fun startTunnelConfChangesJob(tunnelConf: TunnelConf) {
        tunnelRepo.flow
            .flowOn(ioDispatcher)
            .map { storedTunnels -> storedTunnels.firstOrNull { it.id == tunnelConf.id } }
            .filterNotNull()
            // only emit when one of these 3 values change
            .distinctUntilChanged { old, new -> old == new }
            .collect { storedTunnel ->
                if (tunnelConf != storedTunnel) {
                    Timber.d("Config changed for ${storedTunnel.tunName}, bouncing")
                    // let this complete, even after cancel
                    withContext(NonCancellable) {
                        tunnelManager.bounceTunnel(
                            storedTunnel,
                            TunnelStatus.StopReason.CONFIG_CHANGED,
                        )
                    }
                }
            }
    }

    private suspend fun startNetworkMonitorJob() {
        networkMonitor.networkStatusFlow.flowOn(ioDispatcher).collectLatest { status ->
            val isAvailable = status !is NetworkStatus.Disconnected
            isNetworkConnected.value = isAvailable
            Timber.d("Network available: $status")
        }
    }

    private suspend fun startTunnelStatsJob(tunnel: TunnelConf) = coroutineScope {
        while (isActive) {
            tunnelManager.updateTunnelStatistics(tunnel)
            delay(STATS_DELAY)
        }
    }

    private fun startPingJob(tunnel: TunnelConf) =
        lifecycleScope.launch(ioDispatcher) {
            // delay for initial duration
            delay(tunnel.pingInterval ?: Constants.PING_INTERVAL)
            while (isActive) {
                val shouldBounce = shouldBounceTunnel(tunnel)
                val delayMs =
                    if (shouldBounce) {
                        // let this complete, even after cancel
                        withContext(NonCancellable) {
                            tunnelManager.bounceTunnel(tunnel, TunnelStatus.StopReason.PING)
                        }
                        tunnel.pingCooldown ?: Constants.PING_COOLDOWN
                    } else {
                        tunnel.pingInterval ?: Constants.PING_INTERVAL
                    }
                delay(delayMs)
            }
        }

    private suspend fun shouldBounceTunnel(tunnel: TunnelConf): Boolean {
        if (!isNetworkConnected.value) {
            Timber.d("Network disconnected, skipping ping for ${tunnel.tunName}")
            return false
        }
        return runCatching { !tunnel.isTunnelPingable(ioDispatcher) }
            .onFailure { e -> Timber.e(e, "Ping check failed for ${tunnel.tunName}") }
            .getOrDefault(true)
    }

    fun stop() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        serviceManager.handleTunnelServiceDestroy()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun createTunnelNotification(tunnelConf: TunnelConf): Notification {
        return notificationManager.createNotification(
            WireGuardNotification.NotificationChannels.VPN,
            title = "${getString(R.string.tunnel_running)} - ${tunnelConf.tunName}",
            actions =
                listOf(
                    notificationManager.createNotificationAction(
                        NotificationAction.TUNNEL_OFF,
                        tunnelConf.id,
                    )
                ),
        )
    }

    private fun createTunnelsNotification(): Notification {
        return notificationManager.createNotification(
            WireGuardNotification.NotificationChannels.VPN,
            title = "${getString(R.string.tunnel_running)} - ${getString(R.string.multiple)}",
            actions =
                listOf(
                    notificationManager.createNotificationAction(NotificationAction.TUNNEL_OFF, 0)
                ),
        )
    }

    private fun onCreateNotification(): Notification {
        return notificationManager.createNotification(
            WireGuardNotification.NotificationChannels.VPN,
            title = getString(R.string.tunnel_starting),
        )
    }

    // TODO add notification handling and optional log reading for restart on handshake failures
    companion object {
        const val STATS_DELAY = 1_000L
        // ipv6 disabled or block on network
        // Failed to send handshake initiation: write udp [::]"
        // Failed to send data packets: write udp [::]
        // Failed to send data packets: write udp 0.0.0.0:51820
        // Handshake did not complete after 5 seconds, retrying
    }
}
