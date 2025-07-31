package com.zaneschepke.wireguardautotunnel.core.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.VpnService
import android.os.IBinder
import com.zaneschepke.wireguardautotunnel.core.service.autotunnel.AutoTunnelService
import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.di.MainDispatcher
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.util.extensions.requestAutoTunnelTileServiceUpdate
import com.zaneschepke.wireguardautotunnel.util.extensions.requestTunnelTileServiceStateUpdate
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

class ServiceManager
@Inject
constructor(
    private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
    private val appDataRepository: AppDataRepository,
) {

    private val autoTunnelMutex = Mutex()

    private val _tunnelService = MutableStateFlow<TunnelForegroundService?>(null)
    private val _autoTunnelService = MutableStateFlow<AutoTunnelService?>(null)
    val autoTunnelService = _autoTunnelService.asStateFlow()

    private val tunnelServiceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                val binder = service as? TunnelForegroundService.LocalBinder
                _tunnelService.value = binder?.service
                Timber.d("TunnelForegroundService connected")
            }

            override fun onServiceDisconnected(name: ComponentName) {
                _tunnelService.value = null
                Timber.d("TunnelForegroundService disconnected")
            }
        }

    private val autoTunnelServiceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                val binder = service as? AutoTunnelService.LocalBinder
                _autoTunnelService.value = binder?.service
                Timber.d("AutoTunnelService connected")
            }

            override fun onServiceDisconnected(name: ComponentName) {
                _autoTunnelService.value = null
                Timber.d("AutoTunnelService disconnected")
            }
        }

    fun hasVpnPermission(): Boolean {
        return VpnService.prepare(context) == null
    }

    suspend fun startAutoTunnel() {
        autoTunnelMutex.withLock {
            val settings = appDataRepository.settings.get()
            appDataRepository.settings.save(settings.copy(isAutoTunnelEnabled = true))
            if (_autoTunnelService.value != null) return
            withContext(ioDispatcher) {
                val intent = Intent(context, AutoTunnelService::class.java)
                context.startForegroundService(intent)
                context.bindService(intent, autoTunnelServiceConnection, Context.BIND_AUTO_CREATE)
                withContext(mainDispatcher) { updateAutoTunnelTile() }
            }
        }
    }

    suspend fun stopAutoTunnel() {
        autoTunnelMutex.withLock {
            val settings = appDataRepository.settings.get()
            appDataRepository.settings.save(settings.copy(isAutoTunnelEnabled = false))
            if (_autoTunnelService.value == null) return
            _autoTunnelService.value?.let { service ->
                service.stop()
                try {
                    context.unbindService(autoTunnelServiceConnection)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to unbind AutoTunnelService")
                } finally {
                    _tunnelService.value = null
                }
            }
            withContext(mainDispatcher) { updateAutoTunnelTile() }
        }
    }

    suspend fun startTunnelForegroundService() {
        if (_tunnelService.value != null) return
        withContext(ioDispatcher) {
            applicationScope.launch(ioDispatcher) {
                val intent = Intent(context, TunnelForegroundService::class.java)
                context.startForegroundService(intent)
                context.bindService(intent, tunnelServiceConnection, Context.BIND_AUTO_CREATE)
            }
        }
    }

    fun stopTunnelForegroundService() {
        _tunnelService.value?.let { service ->
            service.stop()
            try {
                context.unbindService(tunnelServiceConnection)
            } catch (e: Exception) {
                Timber.e(e, "Failed to stop TunnelForegroundService")
            } finally {
                _tunnelService.value = null
            }
        }
    }

    fun toggleAutoTunnel() {
        applicationScope.launch(ioDispatcher) {
            if (_autoTunnelService.value != null) stopAutoTunnel() else startAutoTunnel()
        }
    }

    fun updateAutoTunnelTile() {
        context.requestAutoTunnelTileServiceUpdate()
    }

    fun updateTunnelTile() {
        context.requestTunnelTileServiceStateUpdate()
    }

    fun handleTunnelServiceDestroy() {
        _tunnelService.update { null }
    }

    fun handleAutoTunnelServiceDestroy() {
        _autoTunnelService.update { null }
    }
}
