package com.zaneschepke.wireguardautotunnel.core.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zaneschepke.wireguardautotunnel.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class KernelReceiver : BroadcastReceiver() {

	@Inject
	@ApplicationScope
	lateinit var applicationScope: CoroutineScope

	@Inject
	lateinit var tunnelRepository: TunnelRepository

	@Inject
	lateinit var serviceManager: ServiceManager

	@Inject
	lateinit var tunnelManager: TunnelManager

	override fun onReceive(context: Context, intent: Intent) {
		val action = intent.action ?: return
		applicationScope.launch {
			if (action == REFRESH_TUNNELS_ACTION) {
				tunnelManager.runningTunnelNames().forEach {
					val tunnel = tunnelRepository.findByTunnelName(it)
					tunnel?.let {
						tunnelRepository.save(it.copy(isActive = true))
					}
				}
				serviceManager.updateTunnelTile()
			}
		}
	}

	companion object {
		const val REFRESH_TUNNELS_ACTION = "com.wireguard.android.action.REFRESH_TUNNEL_STATES"
	}
}
