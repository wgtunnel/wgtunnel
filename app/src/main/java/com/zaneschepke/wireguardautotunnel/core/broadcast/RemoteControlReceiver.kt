package com.zaneschepke.wireguardautotunnel.core.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.domain.repository.AppStateRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.GeneralSettingRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class RemoteControlReceiver : BroadcastReceiver() {

    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    @Inject lateinit var appStateRepository: AppStateRepository
    @Inject lateinit var settingsRepository: GeneralSettingRepository
    @Inject lateinit var tunnelsRepository: TunnelRepository

    @Inject lateinit var tunnelManager: TunnelManager

    enum class Action(private val suffix: String) {
        START_TUNNEL("START_TUNNEL"),
        STOP_TUNNEL("STOP_TUNNEL"),
        START_AUTO_TUNNEL("START_AUTO_TUNNEL"),
        STOP_AUTO_TUNNEL("STOP_AUTO_TUNNEL");

        fun getFullAction(): String {
            return "${Constants.BASE_PACKAGE}.$suffix"
        }

        companion object {
            fun fromAction(action: String): Action? {
                for (a in entries) {
                    if (a.getFullAction() == action) {
                        return a
                    }
                }
                return null
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Timber.i("onReceive")
        val action = intent.action ?: return
        val appAction = Action.fromAction(action) ?: return Timber.w("Unknown action $action")
        applicationScope.launch {
            if (!appStateRepository.isRemoteControlEnabled())
                return@launch Timber.w("Remote control disabled")
            val key =
                appStateRepository.getRemoteKey()
                    ?: return@launch Timber.w("Remote control key missing")
            if (key != intent.getStringExtra(EXTRA_KEY)?.trim())
                return@launch Timber.w("Invalid remote control key")
            when (appAction) {
                Action.START_TUNNEL -> {
                    val tunnelName =
                        intent.getStringExtra(EXTRA_TUN_NAME) ?: return@launch startDefaultTunnel()
                    val tunnel =
                        tunnelsRepository.findByTunnelName(tunnelName)
                            ?: return@launch startDefaultTunnel()
                    tunnelManager.startTunnel(tunnel)
                }
                Action.STOP_TUNNEL -> {
                    val tunnelName =
                        intent.getStringExtra(EXTRA_TUN_NAME)
                            ?: return@launch tunnelManager.stopActiveTunnels()
                    val tunnel =
                        tunnelsRepository.findByTunnelName(tunnelName)
                            ?: return@launch tunnelManager.stopActiveTunnels()
                    tunnelManager.stopTunnel(tunnel.id)
                }
                Action.START_AUTO_TUNNEL -> settingsRepository.updateAutoTunnelEnabled(true)
                Action.STOP_AUTO_TUNNEL -> settingsRepository.updateAutoTunnelEnabled(false)
            }
        }
    }

    private suspend fun startDefaultTunnel() {
        tunnelsRepository.getDefaultTunnel()?.let { tunnel -> tunnelManager.startTunnel(tunnel) }
    }

    companion object {
        const val EXTRA_TUN_NAME = "tunnelName"
        const val EXTRA_KEY = "key"
    }
}
