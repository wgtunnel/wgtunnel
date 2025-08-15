package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendStatus
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.events.BackendError
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.domain.state.AmneziaStatistics
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelStatistics
import com.zaneschepke.wireguardautotunnel.util.extensions.asAmBackendStatus
import com.zaneschepke.wireguardautotunnel.util.extensions.asBackendStatus
import com.zaneschepke.wireguardautotunnel.util.extensions.toBackendError
import kotlinx.coroutines.CoroutineScope
import org.amnezia.awg.backend.Backend
import org.amnezia.awg.backend.BackendException
import org.amnezia.awg.backend.Tunnel
import timber.log.Timber
import javax.inject.Inject
import kotlin.jvm.optionals.getOrNull

class UserspaceTunnel
@Inject
constructor(
    applicationScope: CoroutineScope,
    val serviceManager: ServiceManager,
    val appDataRepository: AppDataRepository,
    private val backend: Backend,
) : BaseTunnel(applicationScope, appDataRepository, serviceManager) {

    override suspend fun startBackend(tunnel: TunnelConf) {
        try {
            updateTunnelStatus(tunnel, TunnelStatus.Starting)
            val amConfig = tunnel.toAmConfig()
            var previousKillSwitch: Backend.BackendStatus? = null
            // prevent dns failures from bringing tuns up when vpn kill switch active
            if (
                amConfig.peers.any { it.endpoint.getOrNull()?.toString()?.isUrl() == true } &&
                    backend.backendStatus is Backend.BackendStatus.KillSwitchActive
            ) {
                previousKillSwitch = backend.backendStatus
                setBackendStatus(BackendStatus.Active)
            }
            backend.setState(tunnel, Tunnel.State.UP, amConfig)
            previousKillSwitch?.let { backend.backendStatus = it }
        } catch (e: BackendException) {
            Timber.e(e, "Failed to start up backend for tunnel ${tunnel.name}")
            throw e.toBackendError()
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Failed to start up backend for tunnel ${tunnel.name}")
            throw BackendError.Config
        }
    }

    override fun stopBackend(tunnel: TunnelConf) {
        Timber.i("Stopping tunnel ${tunnel.name} userspace")
        try {
            backend.setState(tunnel, Tunnel.State.DOWN, tunnel.toAmConfig())
        } catch (e: BackendException) {
            Timber.e(e, "Failed to stop tunnel ${tunnel.id}")
            throw e.toBackendError()
        }
    }

    override fun setBackendStatus(backendStatus: BackendStatus) {
        Timber.d("Setting backend state: $backendStatus")
        try {
            backend.backendStatus = backendStatus.asAmBackendStatus()
        } catch (e: BackendException) {
            throw e.toBackendError()
        }
    }

    override fun getBackendStatus(): BackendStatus {
        return backend.backendStatus.asBackendStatus()
    }

    override suspend fun runningTunnelNames(): Set<String> {
        return backend.runningTunnelNames
    }

    override fun getStatistics(tunnelConf: TunnelConf): TunnelStatistics? {
        return try {
            AmneziaStatistics(backend.getStatistics(tunnelConf))
        } catch (e: Exception) {
            Timber.e(e, "Failed to get stats for ${tunnelConf.tunName}")
            null
        }
    }
}
