package com.zaneschepke.wireguardautotunnel.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
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
    private val appDataRepository: AppDataRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val tunnelManager: TunnelManager,
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
            with(appDataRepository.settings.get()) {
                if (isAutoTunnelEnabled && serviceManager.autoTunnelService.value == null)
                    return@with serviceManager.startAutoTunnel()
                if (tunnelManager.activeTunnels.value.isEmpty())
                    tunnelManager.restorePreviousState()
            }
            Result.success()
        }
}
