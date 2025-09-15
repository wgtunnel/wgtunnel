package com.zaneschepke.wireguardautotunnel.core.shortcut

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.zaneschepke.wireguardautotunnel.core.service.autotunnel.AutoTunnelService
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelProvider
import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.domain.repository.GeneralSettingRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.TunnelRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class ShortcutsActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: GeneralSettingRepository
    @Inject lateinit var tunnelsRepository: TunnelRepository

    @Inject lateinit var tunnelManager: TunnelManager

    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applicationScope.launch {
            val settings = settingsRepository.get()
            if (settings.isShortcutsEnabled) {
                when (intent.getStringExtra(CLASS_NAME_EXTRA_KEY)) {
                    LEGACY_TUNNEL_SERVICE_NAME,
                    TunnelProvider::class.java.simpleName -> {
                        val tunnelName = intent.getStringExtra(TUNNEL_NAME_EXTRA_KEY)
                        Timber.d("Tunnel name extra: $tunnelName")
                        val tunnelConfig =
                            tunnelName?.let { tunnelsRepository.findByTunnelName(it) }
                                ?: tunnelsRepository.getDefaultTunnel()
                        Timber.d("Shortcut action on name: ${tunnelConfig?.tunName}")
                        tunnelConfig?.let {
                            when (intent.action) {
                                Action.START.name -> tunnelManager.startTunnel(it)
                                Action.STOP.name -> tunnelManager.stopActiveTunnels()
                                else -> Unit
                            }
                        }
                    }
                    AutoTunnelService::class.java.simpleName,
                    LEGACY_AUTO_TUNNEL_SERVICE_NAME -> {
                        when (intent.action) {
                            Action.START.name -> settingsRepository.updateAutoTunnelEnabled(true)
                            Action.STOP.name -> settingsRepository.updateAutoTunnelEnabled(false)
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
