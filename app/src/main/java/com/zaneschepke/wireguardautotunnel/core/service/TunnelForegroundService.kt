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
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelMonitor
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.enums.NotificationAction
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.distinctByKeys
import dagger.hilt.android.AndroidEntryPoint
import io.ktor.util.collections.*
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class TunnelForegroundService : LifecycleService() {

    @Inject lateinit var notificationManager: NotificationManager

    @Inject lateinit var serviceManager: ServiceManager

    @Inject lateinit var tunnelManager: TunnelManager

    @Inject lateinit var tunnelMonitor: TunnelMonitor

    @Inject @IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher

    @Inject lateinit var appDataRepository: AppDataRepository

    class LocalBinder(val service: TunnelForegroundService) : Binder()

    private val tunnelJobs = ConcurrentMap<TunnelConf, Job>()

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
                val activeTunConfigs = activeTunnels.keys
                val obsoleteJobs = tunnelJobs.keys - activeTunConfigs
                obsoleteJobs.forEach { tunnelConf -> tunnelJobs[tunnelConf]?.cancel() }
                activeTunConfigs.forEach { tun ->
                    if (tunnelJobs.containsKey(tun)) return@forEach
                    tunnelJobs[tun] = launch { tunnelMonitor.startMonitoring(tun, true) }
                }
                updateServiceNotification(activeTunnels)
            }
        }

    // TODO Would be cool to have this include kill switch
    private fun updateServiceNotification(activeTunnels: Map<TunnelConf, TunnelState>) {
        val notification =
            when (activeTunnels.size) {
                0 -> onCreateNotification()
                1 -> createTunnelNotification(activeTunnels.keys.first())
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
        Timber.d("Stop called")
        tunnelJobs.forEach { it.value.cancel() }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        tunnelJobs.forEach { it.value.cancel() }
        serviceManager.handleTunnelServiceDestroy()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        Timber.d("onDestroy")
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
            onGoing = true,
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
