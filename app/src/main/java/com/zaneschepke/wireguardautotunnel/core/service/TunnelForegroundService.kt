package com.zaneschepke.wireguardautotunnel.core.service

import android.app.Notification
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.core.notification.NotificationManager
import com.zaneschepke.wireguardautotunnel.core.notification.WireGuardNotification
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelMonitor
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.enums.NotificationAction
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.distinctByKeys
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@AndroidEntryPoint
class TunnelForegroundService : LifecycleService() {

    @Inject lateinit var notificationManager: NotificationManager

    @Inject lateinit var serviceManager: ServiceManager

    @Inject lateinit var tunnelManager: TunnelManager

    @Inject lateinit var tunnelMonitor: TunnelMonitor

    @Inject @IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher

    private val tunnelJobs = ConcurrentHashMap<TunnelConf, Unit>()

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
                // Synchronize jobs with active tunnels
                synchronizeJobs(activeTunnels)
                updateServiceNotification()
                if (activeTunnels.isEmpty()) {
                    stop()
                }
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
        if (activeTunnels.isEmpty()) return clearAllJobs()
        // Stop jobs for tunnels not in activeTunnels
        val tunnelsToStop = tunnelJobs.keys - activeTunnels.keys
        tunnelsToStop.forEach { tun -> stopTunnelJobs(tun) }
    }

    private fun clearAllJobs() {
        tunnelJobs.keys.forEach { tun ->
            tunnelMonitor.stopMonitoring(tun)
        }
        tunnelJobs.clear()
    }

    private fun stopTunnelJobs(tun: TunnelConf) {
        tunnelMonitor.stopMonitoring(tun)
        tunnelJobs.remove(tun)
        Timber.d("Stopped all tunnel jobs for ${tun.tunName}")
    }

    private fun startNewJobs(activeTunnels: Map<TunnelConf, TunnelState>) {
        val tunnelsToStart = activeTunnels.keys - tunnelJobs.keys
        tunnelsToStart.forEach { tun ->
            tunnelMonitor.startMonitoring(lifecycleScope, tun)
            tunnelJobs[tun] = Unit
            Timber.d("Started tunnel jobs for ${tun.tunName}")
        }
    }

    // TODO Would be cool to have this include kill switch
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
}