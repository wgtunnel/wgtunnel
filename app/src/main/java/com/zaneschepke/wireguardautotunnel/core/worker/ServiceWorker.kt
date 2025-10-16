package com.zaneschepke.wireguardautotunnel.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.repository.AutoTunnelSettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber

@HiltWorker
class ServiceWorker
@AssistedInject
constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val serviceManager: ServiceManager,
    private val autoTunnelSettingsRepository: AutoTunnelSettingsRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "service_worker"

        fun stop(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
        }

        fun start(context: Context) {
            val periodicWorkRequest =
                PeriodicWorkRequestBuilder<ServiceWorker>(
                        repeatInterval = 15,
                        repeatIntervalTimeUnit = TimeUnit.MINUTES,
                    )
                    .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    TAG,
                    ExistingPeriodicWorkPolicy.KEEP,
                    periodicWorkRequest,
                )
        }
    }

    override suspend fun doWork(): Result =
        withContext(ioDispatcher) {
            Timber.i("Service worker started")
            with(autoTunnelSettingsRepository.getAutoTunnelSettings()) {
                Timber.i("Checking to see if auto-tunnel has been killed by system")
                if (isAutoTunnelEnabled && serviceManager.autoTunnelService.value == null) {
                    Timber.i("Service has been killed by system, restoring.")
                    serviceManager.startAutoTunnelService()
                }
            }
            Result.success()
        }
}
