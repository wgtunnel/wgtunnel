package com.zaneschepke.wireguardautotunnel.core.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
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
		val action = intent.action ?: return
		if (action != Intent.ACTION_BOOT_COMPLETED &&
			action != Intent.ACTION_MY_PACKAGE_REPLACED &&
			action != "com.htc.intent.action.QUICKBOOT_POWERON"
		) {
			return
		}

		Timber.d("RestartReceiver triggered with action: ${intent.action}")
		applicationScope.launch(ioDispatcher) {
			serviceManager.updateTunnelTile()
			serviceManager.updateAutoTunnelTile()
			val settings = appDataRepository.settings.get()
			if (settings.isRestoreOnBootEnabled) {
				if (settings.isAutoTunnelEnabled && !serviceManager.autoTunnelActive.value) {
					Timber.d("Starting auto-tunnel on boot/update")
					serviceManager.startAutoTunnel(true)
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
