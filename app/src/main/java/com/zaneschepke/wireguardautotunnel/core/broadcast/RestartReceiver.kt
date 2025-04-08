package com.zaneschepke.wireguardautotunnel.core.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
	@Inject
	lateinit var appDataRepository: AppDataRepository

	@Inject
	@ApplicationScope
	lateinit var applicationScope: CoroutineScope

	@Inject
	lateinit var serviceManager: ServiceManager

	@Inject
	lateinit var tunnelManager: TunnelManager

	@Inject
	@IoDispatcher
	lateinit var ioDispatcher: CoroutineDispatcher

	override fun onReceive(context: Context, intent: Intent) {
		Timber.d("RestartReceiver triggered with action: ${intent.action}")
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
