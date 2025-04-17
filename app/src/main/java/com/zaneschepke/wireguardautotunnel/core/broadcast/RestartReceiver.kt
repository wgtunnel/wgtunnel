package com.zaneschepke.wireguardautotunnel.core.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.util.extensions.isRunningOnTv
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class RestartReceiver : BroadcastReceiver() {
    @Inject lateinit var appDataRepository: AppDataRepository

    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    @Inject lateinit var serviceManager: ServiceManager

    @Inject lateinit var tunnelManager: TunnelManager

    @Inject @IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("RestartReceiver triggered with action: ${intent.action}")
        // screen on for Android TV only to help with sleep shutdowns
        if (intent.action == Intent.ACTION_SCREEN_ON && !context.isRunningOnTv()) return
        serviceManager.updateTunnelTile()
        serviceManager.updateAutoTunnelTile()
        applicationScope.launch(ioDispatcher) {
            val settings = appDataRepository.settings.get()
            if (settings.isRestoreOnBootEnabled) {
                if (settings.isAutoTunnelEnabled && !serviceManager.autoTunnelActive.value) {
                    Timber.d("Starting auto-tunnel on boot/update")
                    serviceManager.startAutoTunnel()
                } else {
                    Timber.d("Restoring previous tunnel state")
                    tunnelManager.restorePreviousState()
                }
            } else {
                Timber.d("Restore on boot disabled, skipping")
            }
        }
    }
}
