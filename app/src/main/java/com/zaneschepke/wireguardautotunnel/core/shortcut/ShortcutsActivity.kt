package com.zaneschepke.wireguardautotunnel.core.shortcut

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.core.service.autotunnel.AutoTunnelService
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ShortcutsActivity : ComponentActivity() {
	@Inject
	lateinit var appDataRepository: AppDataRepository

	@Inject
	lateinit var serviceManager: ServiceManager

	@Inject
	@ApplicationScope
	lateinit var applicationScope: CoroutineScope

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		applicationScope.launch {
			val settings = appDataRepository.settings.get()
			if (settings.isShortcutsEnabled) {
				when (intent.getStringExtra(CLASS_NAME_EXTRA_KEY)) {
					LEGACY_TUNNEL_SERVICE_NAME, TunnelProvider::class.java.simpleName -> {
						val tunnelName = intent.getStringExtra(TUNNEL_NAME_EXTRA_KEY)
						Timber.d("Tunnel name extra: $tunnelName")
						val tunnelConfig = tunnelName?.let {
							appDataRepository.tunnels.getAll()
								.firstOrNull { it.tunName == tunnelName }
						} ?: appDataRepository.getStartTunnelConfig()
						Timber.d("Shortcut action on name: ${tunnelConfig?.tunName}")
// 						tunnelConfig?.let {
// 							when (intent.action) {
// 								Action.START.name -> tunnelService.get().startTunnel(it)
// 								Action.STOP.name -> tunnelService.get().stopTunnel()
// 								else -> Unit
// 							}
// 						}
					}
					AutoTunnelService::class.java.simpleName, LEGACY_AUTO_TUNNEL_SERVICE_NAME -> {
						when (intent.action) {
							Action.START.name -> serviceManager.startAutoTunnel(true)
							Action.STOP.name -> serviceManager.stopAutoTunnel()
						}
					}
				}
			}
		}
		finish()
	}

	enum class Action {
		START,
		STOP,
	}

	companion object {
		const val LEGACY_TUNNEL_SERVICE_NAME = "WireGuardTunnelService"
		const val LEGACY_AUTO_TUNNEL_SERVICE_NAME = "WireGuardConnectivityWatcherService"
		const val TUNNEL_NAME_EXTRA_KEY = "tunnelName"
		const val CLASS_NAME_EXTRA_KEY = "className"
	}
}
