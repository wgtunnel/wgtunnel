package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendState
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.domain.state.AmneziaStatistics
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelStatistics
import com.zaneschepke.wireguardautotunnel.util.extensions.asAmBackendState
import com.zaneschepke.wireguardautotunnel.util.extensions.asBackendState
import com.zaneschepke.wireguardautotunnel.util.extensions.toBackendError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import org.amnezia.awg.backend.Backend
import org.amnezia.awg.backend.BackendException
import org.amnezia.awg.backend.Tunnel
import timber.log.Timber
import javax.inject.Inject
import kotlin.jvm.optionals.getOrNull

class UserspaceTunnel @Inject constructor(
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
	@ApplicationScope private val applicationScope: CoroutineScope,
	serviceManager: ServiceManager,
	val appDataRepository: AppDataRepository,
	private val backend: Backend,
) : BaseTunnel(ioDispatcher, applicationScope, appDataRepository, serviceManager) {

	private var previousBackendState: Pair<BackendState, Boolean>? = null

	override suspend fun startBackend(tunnel: TunnelConf) {
		try {
			updateTunnelStatus(tunnel, TunnelStatus.Starting)
			// stop vpn kill switch if we need to resolve DNS for peer endpoints
			// endpoint resolved by DNS
			val amConfig = tunnel.toAmConfig()
			if (amConfig.peers.any { it.endpoint.getOrNull()?.toString()?.isUrl() == true } &&
				backend.backendState.asBackendState() == BackendState.KILL_SWITCH_ACTIVE
			) {
				val bypassLan = appDataRepository.settings.get().isLanOnKillSwitchEnabled
				previousBackendState = Pair(BackendState.KILL_SWITCH_ACTIVE, bypassLan)
				setBackendState(BackendState.SERVICE_ACTIVE, emptyList())
			}
			backend.setState(tunnel, Tunnel.State.UP, amConfig)
		} catch (e: BackendException) {
			Timber.e(e, "Failed to start up backend for tunnel ${tunnel.name}")
			throw e.toBackendError()
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

	override fun setBackendState(backendState: BackendState, allowedIps: Collection<String>) {
		Timber.d("Setting backend state: $backendState with allowedIps: $allowedIps")
		try {
			backend.setBackendState(backendState.asAmBackendState(), allowedIps)
			previousBackendState?.let { (state, lanEnabled) ->
				Timber.d("Restoring kill switch configuration")
				val lan = if (lanEnabled) TunnelConf.LAN_BYPASS_ALLOWED_IPS else emptyList()
				backend.setBackendState(state.asAmBackendState(), lan)
			}
		} catch (e: BackendException) {
			throw e.toBackendError()
		}
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
