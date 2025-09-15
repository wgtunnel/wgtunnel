package com.zaneschepke.wireguardautotunnel.core.service.tile

import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.domain.repository.TunnelRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class TunnelControlTile : TileService(), LifecycleOwner {

    @Inject lateinit var tunnelsRepository: TunnelRepository

    @Inject lateinit var serviceManager: ServiceManager

    @Inject lateinit var tunnelManager: TunnelManager

    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)

    private var isCollecting = false

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    override fun onStartListening() {
        super.onStartListening()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        Timber.d("Start listening called for tunnel tile")
        if (isCollecting) return
        isCollecting = true
        lifecycleScope.launch { tunnelManager.activeTunnels.collect { updateTileState() } }
    }

    private suspend fun updateTileState() {
        try {
            val tunnels = tunnelsRepository.getAll()
            if (tunnels.isEmpty()) {
                setUnavailable()
                return
            }

            val activeTunnels =
                tunnelManager.activeTunnels.value.filter { it.value.status.isUpOrStarting() }

            when {
                activeTunnels.isNotEmpty() -> {
                    val activeIds = activeTunnels.map { it.key }
                    // TODO improvements would be needed to make this work well with toggling
                    // multiple tunnels
                    // this would be better managed elsewhere
                    WireGuardAutoTunnel.setLastActiveTunnels(activeIds)
                    val activeTunNames =
                        tunnels.filter { activeTunnels.keys.contains(it.id) }.map { it.tunName }
                    updateTileForActiveTunnels(activeTunNames)
                }
                else -> updateTileForLastActiveTunnels()
            }
        } catch (e: Exception) {
            setUnavailable()
        }
    }

    private fun updateTileForActiveTunnels(activeTunnelNames: List<String>) {
        val tileName =
            when (activeTunnelNames.size) {
                1 -> activeTunnelNames[0]
                else -> getString(R.string.multiple)
            }
        updateTile(tileName, true)
    }

    private suspend fun updateTileForLastActiveTunnels() {
        val lastActiveIds = WireGuardAutoTunnel.getLastActiveTunnels()
        when {
            lastActiveIds.isEmpty() -> {
                tunnelsRepository.getStartTunnel()?.let { config ->
                    updateTile(config.tunName, false)
                } ?: setUnavailable()
            }
            lastActiveIds.size > 1 -> updateTile(getString(R.string.multiple), false)
            else -> {
                val tunnelId = lastActiveIds.first()
                tunnelsRepository.getById(tunnelId)?.let { tunnel ->
                    updateTile(tunnel.tunName, false)
                } ?: setUnavailable()
            }
        }
    }

    override fun onClick() {
        super.onClick()
        unlockAndRun {
            lifecycleScope.launch {
                if (tunnelManager.activeTunnels.value.isNotEmpty())
                    return@launch tunnelManager.stopActiveTunnels()
                val lastActive = WireGuardAutoTunnel.getLastActiveTunnels()
                if (lastActive.isEmpty()) {
                    tunnelsRepository.getStartTunnel()?.let { tunnelManager.startTunnel(it) }
                } else {
                    lastActive.forEach { id ->
                        tunnelsRepository.getById(id)?.let { tunnelManager.startTunnel(it) }
                    }
                }
            }
        }
    }

    private fun setActive() {
        runCatching {
            qsTile.state = Tile.STATE_ACTIVE
            qsTile.updateTile()
        }
    }

    private fun setInactive() {
        runCatching {
            qsTile.state = Tile.STATE_INACTIVE
            qsTile.updateTile()
        }
    }

    private fun setUnavailable() {
        runCatching {
            qsTile.state = Tile.STATE_UNAVAILABLE
            setTileDescription("")
            qsTile.updateTile()
        }
    }

    private fun setTileDescription(description: String) {
        runCatching {
            if (qsTile == null) return@runCatching
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                qsTile.subtitle = description
                qsTile.stateDescription = description
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                qsTile.subtitle = description
            }
            qsTile.updateTile()
        }
    }

    /* This works around an annoying unsolved frameworks bug some people are hitting. */
    override fun onBind(intent: Intent): IBinder? {
        var ret: IBinder? = null
        try {
            ret = super.onBind(intent)
        } catch (_: Throwable) {
            Timber.e("Failed to bind to TunnelControlTile")
        }
        return ret
    }

    private fun updateTile(name: String, active: Boolean) {
        runCatching {
                setTileDescription(name)
                if (active) return setActive()
                setInactive()
            }
            .onFailure { Timber.e(it) }
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
}
