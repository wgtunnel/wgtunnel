package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.BackendException
import com.wireguard.android.backend.Tunnel
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.di.Kernel
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendMode
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.events.BackendCoreException
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelStatistics
import com.zaneschepke.wireguardautotunnel.domain.state.WireGuardStatistics
import com.zaneschepke.wireguardautotunnel.util.extensions.toBackendCoreException
import kotlinx.coroutines.CoroutineScope
import timber.log.Timber
import javax.inject.Inject

class KernelTunnel
@Inject
constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    serviceManager: ServiceManager,
    appDataRepository: AppDataRepository,
    @Kernel private val backend: Backend,
) : BaseTunnel(applicationScope, appDataRepository, serviceManager) {

    override fun getStatistics(tunnelConf: TunnelConf): TunnelStatistics? {
        return try {
            WireGuardStatistics(backend.getStatistics(tunnelConf))
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }

    override suspend fun startBackend(tunnel: TunnelConf) {
        // name too long for kernel mode
        if (!tunnel.isNameKernelCompatible) throw BackendCoreException.TunnelNameTooLong
        try {
            updateTunnelStatus(tunnel, TunnelStatus.Starting)
            backend.setState(tunnel, Tunnel.State.UP, tunnel.toWgConfig())
        } catch (e: BackendException) {
            Timber.e(e, "Failed to start up backend for tunnel ${tunnel.name}")
            throw e.toBackendCoreException()
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Failed to start up backend for tunnel ${tunnel.name}")
            throw BackendCoreException.Config
        }
    }

    override fun stopBackend(tunnel: TunnelConf) {
        Timber.i("Stopping tunnel ${tunnel.id} kernel")
        try {
            backend.setState(tunnel, Tunnel.State.DOWN, tunnel.toWgConfig())
        } catch (e: BackendException) {
            throw e.toBackendCoreException()
        }
    }

    override fun setBackendMode(backendMode: BackendMode) {
        Timber.w("Not yet implemented for kernel")
    }

    override fun getBackendMode(): BackendMode {
        return BackendMode.Inactive
    }

    override suspend fun runningTunnelNames(): Set<String> {
        return backend.runningTunnelNames
    }
}
