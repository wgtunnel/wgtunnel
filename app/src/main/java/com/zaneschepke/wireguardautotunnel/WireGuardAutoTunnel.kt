package com.zaneschepke.wireguardautotunnel

import android.app.Application
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.wireguard.android.backend.GoBackend
import com.zaneschepke.logcatter.LogReader
import com.zaneschepke.wireguardautotunnel.core.notification.NotificationMonitor
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.core.worker.ServiceWorker
import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.di.MainDispatcher
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendStatus
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.util.LocaleUtil
import com.zaneschepke.wireguardautotunnel.util.ReleaseTree
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class WireGuardAutoTunnel : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    @Inject lateinit var logReader: LogReader

    @Inject lateinit var appDataRepository: AppDataRepository

    @Inject @IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher

    @Inject @MainDispatcher lateinit var mainDispatcher: CoroutineDispatcher

    @Inject lateinit var notificationMonitor: NotificationMonitor

    @Inject lateinit var tunnelManager: TunnelManager

    override fun onCreate() {
        super.onCreate()
        instance = this
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            StrictMode.setThreadPolicy(
                ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
        } else {
            Timber.plant(ReleaseTree())
        }

        GoBackend.setAlwaysOnCallback {
            applicationScope.launch {
                val settings = appDataRepository.settings.get()
                if (settings.isAlwaysOnVpnEnabled) {
                    val tunnel = appDataRepository.getPrimaryOrFirstTunnel()
                    tunnel?.let { tunnelManager.startTunnel(it) }
                } else {
                    Timber.w("Always-on VPN is not enabled in app settings")
                }
            }
        }

        ServiceWorker.start(this)

        applicationScope.launch {
            launch { notificationMonitor.handleApplicationNotifications() }
            appDataRepository.appState.getLocale()?.let {
                withContext(mainDispatcher) { LocaleUtil.changeLocale(it) }
            }
            appDataRepository.appState.isLocalLogsEnabled().let { enabled ->
                if (enabled) logReader.start()
            }
        }
    }

    override fun onTerminate() {
        applicationScope.cancel()
        tunnelManager.setBackendStatus(BackendStatus.Inactive)
        super.onTerminate()
    }

    companion object {

        private val _uiActive = MutableStateFlow(false)

        val uiActive: StateFlow<Boolean>
            get() = _uiActive

        fun setUiActive(active: Boolean) {
            _uiActive.update { active }
        }

        @Volatile private var lastActiveTunnels: List<Int> = emptyList()

        @Synchronized
        fun getLastActiveTunnels(): List<Int> {
            return lastActiveTunnels
        }

        @Synchronized
        fun setLastActiveTunnels(newTunnels: List<Int>) {
            lastActiveTunnels = newTunnels
        }

        lateinit var instance: WireGuardAutoTunnel
            private set
    }
}
