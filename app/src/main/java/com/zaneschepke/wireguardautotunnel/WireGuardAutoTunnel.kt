package com.zaneschepke.wireguardautotunnel

import android.app.Application
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.zaneschepke.logcatter.LogReader
import com.zaneschepke.wireguardautotunnel.core.notification.NotificationMonitor
import com.zaneschepke.wireguardautotunnel.core.worker.ServiceWorker
import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.repository.MonitoringSettingsRepository
import com.zaneschepke.wireguardautotunnel.util.ReleaseTree
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltAndroidApp
class WireGuardAutoTunnel : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    @Inject lateinit var logReader: LogReader

    @Inject @IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher

    @Inject lateinit var monitoringRepository: MonitoringSettingsRepository

    @Inject lateinit var notificationMonitor: NotificationMonitor

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

        applicationScope.launch(ioDispatcher) {
            launch {
                val monitoringSettings = monitoringRepository.getMonitoringSettings()
                if (monitoringSettings.isLocalLogsEnabled) logReader.start()
            }
            launch { notificationMonitor.handleApplicationNotifications() }
        }

        ServiceWorker.start(this)
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
