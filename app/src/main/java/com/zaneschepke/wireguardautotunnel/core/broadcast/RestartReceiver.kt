package com.zaneschepke.wireguardautotunnel.core.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zaneschepke.logcatter.LogReader
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class RestartReceiver : BroadcastReceiver() {
    @Inject lateinit var appDataRepository: AppDataRepository

    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    @Inject lateinit var serviceManager: ServiceManager

    // injecting this should let tunnelManger handle clean startup
    @Inject lateinit var tunnelManager: TunnelManager

    @Inject lateinit var logReader: LogReader

    @Inject @IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("RestartReceiver triggered with action: ${intent.action}")
        serviceManager.updateTunnelTile()
        serviceManager.updateAutoTunnelTile()
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED)
            applicationScope.launch(ioDispatcher) { logReader.deleteAndClearLogs() }
    }
}
