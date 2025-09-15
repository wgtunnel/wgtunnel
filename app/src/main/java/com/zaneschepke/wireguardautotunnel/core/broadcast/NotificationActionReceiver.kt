package com.zaneschepke.wireguardautotunnel.core.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zaneschepke.wireguardautotunnel.core.notification.NotificationManager
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.domain.enums.NotificationAction
import com.zaneschepke.wireguardautotunnel.domain.repository.GeneralSettingRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.TunnelRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var tunnelManager: TunnelManager

    @Inject lateinit var tunnelRepository: TunnelRepository

    @Inject lateinit var settingsRepository: GeneralSettingRepository

    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        applicationScope.launch {
            when (intent.action) {
                NotificationAction.AUTO_TUNNEL_OFF.name ->
                    settingsRepository.updateAutoTunnelEnabled(false)
                NotificationAction.TUNNEL_OFF.name -> {
                    val tunnelId = intent.getIntExtra(NotificationManager.EXTRA_ID, 0)
                    if (tunnelId == STOP_ALL_TUNNELS_ID)
                        return@launch tunnelManager.stopActiveTunnels()
                    tunnelRepository.getById(tunnelId)?.let { tunnelManager.stopTunnel(it.id) }
                }
            }
        }
    }

    companion object {
        const val STOP_ALL_TUNNELS_ID = 0
    }
}
