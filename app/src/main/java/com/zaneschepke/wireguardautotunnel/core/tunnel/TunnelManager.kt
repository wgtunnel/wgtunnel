package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.data.entity.TunnelConfig as Entity
import com.zaneschepke.wireguardautotunnel.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.di.*
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendMode
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.events.BackendCoreException
import com.zaneschepke.wireguardautotunnel.domain.events.BackendMessage
import com.zaneschepke.wireguardautotunnel.domain.model.GeneralSettings
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.domain.repository.AutoTunnelSettingsRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.GeneralSettingRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.domain.state.LogHealthState
import com.zaneschepke.wireguardautotunnel.domain.state.PingState
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelStatistics
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalAtomicApi::class)
class TunnelManager
@Inject
constructor(
    @Kernel private val kernelTunnel: TunnelProvider,
    @Userspace private val userspaceTunnel: TunnelProvider,
    @ProxyUserspace private val proxyUserspaceTunnel: TunnelProvider,
    private val serviceManager: ServiceManager,
    private val settingsRepository: GeneralSettingRepository,
    private val autoTunnelSettingsRepository: AutoTunnelSettingsRepository,
    private val tunnelsRepository: TunnelRepository,
    private val tunnelMonitor: TunnelMonitor,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TunnelProvider {

    private val monitoringMutex = Mutex()
    private val monitoringJobs = ConcurrentHashMap<Int, Job>()

    private data class SideEffectState(
        val activeTuns: Map<Int, TunnelState>,
        val tuns: List<TunnelConfig>,
        val settings: GeneralSettings,
        val previouslyActive: Map<Int, TunnelState>,
    )

    private data class SideEffectWithCondition(
        val effect: suspend (SideEffectState) -> Unit,
        val condition: (SideEffectState) -> Boolean,
    )

    private val settings: StateFlow<GeneralSettings> =
        settingsRepository.flow
            .filterNotNull()
            .stateIn(
                scope = applicationScope.plus(ioDispatcher),
                started = SharingStarted.Eagerly,
                initialValue = GeneralSettings(),
            )

    private val tunnels: StateFlow<List<TunnelConfig>> =
        tunnelsRepository.flow.stateIn(
            scope = applicationScope.plus(ioDispatcher),
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    private suspend fun getSettings(): GeneralSettings =
        settingsRepository.flow.filterNotNull().first { it != GeneralSettings() }

    private suspend fun getTunnels(): List<TunnelConfig> =
        tunnelsRepository.flow.first { it.isNotEmpty() }

    private val tunnelProviderFlow: StateFlow<TunnelProvider> = run {
        val currentBackend = AtomicReference(userspaceTunnel)
        val currentSettings = AtomicReference(GeneralSettings())
        val initialEmit = AtomicBoolean(true)

        settingsRepository.flow
            .filterNotNull()
            // ignore default state
            .filterNot { it == GeneralSettings() }
            .distinctUntilChanged { old, new ->
                old.appMode == new.appMode &&
                    old.isLanOnKillSwitchEnabled == new.isLanOnKillSwitchEnabled
            }
            .map { settings ->
                Timber.d("App mode changes with ${settings.appMode}")
                val backend =
                    when (settings.appMode) {
                        AppMode.VPN -> userspaceTunnel
                        AppMode.PROXY -> proxyUserspaceTunnel
                        AppMode.LOCK_DOWN -> proxyUserspaceTunnel
                        AppMode.KERNEL -> kernelTunnel
                    }
                settings to backend
            }
            .onEach { (settings, newBackend) ->
                val isInitialEmit = initialEmit.exchange(false)
                val previousBackend = currentBackend.exchange(newBackend)
                val previousSettings = currentSettings.exchange(settings)

                if ((previousSettings.appMode != settings.appMode) && !isInitialEmit) {
                    handleModeChangeCleanup(previousBackend, previousSettings.appMode)
                }
                if (settings.appMode == AppMode.LOCK_DOWN) {
                    handleLockDownModeInit(settings.isLanOnKillSwitchEnabled)
                }
            }
            .map { (_, backend) -> backend }
            .stateIn(
                scope = applicationScope.plus(ioDispatcher),
                started = SharingStarted.Eagerly,
                initialValue = userspaceTunnel,
            )
    }

    override val activeTunnels: StateFlow<Map<Int, TunnelState>> = run {
        val activeTunsReference: AtomicReference<Map<Int, TunnelState>> =
            AtomicReference(emptyMap())

        tunnelProviderFlow
            .flatMapLatest { backend ->
                combine(
                    backend.activeTunnels,
                    tunnelsRepository.flow,
                    settingsRepository.flow.filterNotNull(),
                ) { activeTuns, tuns, settings ->
                    Triple(activeTuns, tuns, settings)
                }
            }
            .onStart { handleStateRestore() }
            .onEach { (activeTuns, tuns, settings) ->
                val previouslyActive = activeTunsReference.exchange(activeTuns)
                val state = SideEffectState(activeTuns, tuns, settings, previouslyActive)

                applicationScope.launch(ioDispatcher) {
                    supervisorScope {
                        val sideEffects =
                            listOf(
                                SideEffectWithCondition(
                                    effect = { s ->
                                        handleTunnelServiceChange(s.settings.appMode, s.activeTuns)
                                    },
                                    condition = { s ->
                                        s.activeTuns.size != s.previouslyActive.size
                                    },
                                ),
                                SideEffectWithCondition(
                                    effect = { s ->
                                        handleTunnelsActiveChange(
                                            s.previouslyActive,
                                            s.activeTuns,
                                            s.tuns,
                                        )
                                    },
                                    condition = { s ->
                                        s.activeTuns.size != s.previouslyActive.size
                                    },
                                ),
                                // TODO Not for kernel mode for now
                                SideEffectWithCondition(
                                    effect = { s ->
                                        handleTunnelMonitoringChanges(s.activeTuns, s.tuns)
                                    },
                                    condition = { s ->
                                        s.tuns.any {
                                            it.restartOnPingFailure &&
                                                s.activeTuns.keys.contains(it.id)
                                        } && s.settings.appMode != AppMode.KERNEL
                                    },
                                ),
                                SideEffectWithCondition(
                                    effect = { s ->
                                        handleFullTunnelMonitoring(s.activeTuns, s.tuns, s.settings)
                                    },
                                    condition = { s ->
                                        s.activeTuns.keys != s.previouslyActive.keys
                                    },
                                ),
                            )

                        sideEffects
                            .filter { it.condition(state) }
                            .forEach { sideEffect ->
                                launch {
                                    try {
                                        sideEffect.effect(state)
                                    } catch (e: Exception) {
                                        Timber.e(e, "Side effect failed")
                                    }
                                }
                            }
                    }
                }
            }
            .map { (activeTuns, _, _) -> activeTuns }
            .stateIn(
                scope = applicationScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyMap(),
            )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val errorEvents: SharedFlow<Pair<String, BackendCoreException>> =
        tunnelProviderFlow
            .flatMapLatest { it.errorEvents }
            .shareIn(
                scope = applicationScope.plus(ioDispatcher),
                started = SharingStarted.Eagerly,
                replay = 0,
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    override val messageEvents: SharedFlow<Pair<String, BackendMessage>> =
        tunnelProviderFlow
            .flatMapLatest { it.messageEvents }
            .shareIn(
                scope = applicationScope.plus(ioDispatcher),
                started = SharingStarted.Eagerly,
                replay = 0,
            )

    override fun getStatistics(tunnelId: Int): TunnelStatistics? {
        return tunnelProviderFlow.value.getStatistics(tunnelId)
    }

    override suspend fun startTunnel(tunnelConfig: TunnelConfig) {
        val provider = tunnelProviderFlow.value
        val isKernel = provider is KernelTunnel

        if (!isKernel && activeTunnels.value.isNotEmpty()) {
            stopActiveTunnels()
            withTimeoutOrNull(BaseTunnel.STARTUP_TIMEOUT_MS) {
                activeTunnels.first { it.isEmpty() }
            } ?: run { activeTunnels.value.keys.forEach { id -> provider.forceStopTunnel(id) } }
        }
        val runConfig =
            tunnelConfig.run {
                if (getSettings().isTunnelGlobalsEnabled) {
                    val globalTunnel =
                        getTunnels().firstOrNull { it.name == Entity.GLOBAL_CONFIG_NAME }
                            ?: return@run this
                    return@run copyWithGlobalValues(globalTunnel)
                }
                this
            }
        tunnelProviderFlow.value.startTunnel(runConfig)
    }

    override suspend fun stopTunnel(tunnelId: Int) {
        tunnelProviderFlow.value.stopTunnel(tunnelId)
    }

    override suspend fun forceStopTunnel(tunnelId: Int) {
        tunnelProviderFlow.value.forceStopTunnel(tunnelId)
    }

    override suspend fun stopActiveTunnels() {
        tunnelProviderFlow.value.stopActiveTunnels()
    }

    override fun setBackendMode(backendMode: BackendMode) {
        tunnelProviderFlow.value.setBackendMode(backendMode)
    }

    override fun getBackendMode(): BackendMode {
        return tunnelProviderFlow.value.getBackendMode()
    }

    override suspend fun runningTunnelNames(): Set<String> {
        return tunnelProviderFlow.value.runningTunnelNames()
    }

    override fun handleDnsReresolve(tunnelConfig: TunnelConfig): Boolean {
        return tunnelProviderFlow.value.handleDnsReresolve(tunnelConfig)
    }

    override suspend fun updateTunnelStatus(
        tunnelId: Int,
        status: TunnelStatus?,
        stats: TunnelStatistics?,
        pingStates: Map<String, PingState>?,
        logHealthState: LogHealthState?,
    ) {
        tunnelProviderFlow.value.updateTunnelStatus(
            tunnelId,
            status,
            stats,
            pingStates,
            logHealthState,
        )
    }

    private suspend fun handleTunnelServiceChange(
        appMode: AppMode,
        activeTuns: Map<Int, TunnelState>,
    ) {
        if (activeTuns.isEmpty()) serviceManager.stopTunnelService()
        if (activeTuns.isNotEmpty() && serviceManager.tunnelService.value == null)
            serviceManager.startTunnelService(appMode)
        serviceManager.updateTunnelTile()
    }

    private fun handleLockDownModeInit(withLanBypass: Boolean) {
        val allowedIps = if (withLanBypass) TunnelConfig.IPV4_PUBLIC_NETWORKS else emptySet()
        try {
            // TODO handle situation where they don't have vpn permission, request it
            if (serviceManager.hasVpnPermission()) {
                proxyUserspaceTunnel.setBackendMode(BackendMode.KillSwitch(allowedIps))
            }
        } catch (e: BackendCoreException) {
            // TODO expose this error to user
            Timber.e(e)
        }
    }

    private suspend fun handleModeChangeCleanup(
        previousBackend: TunnelProvider,
        previousAppMode: AppMode,
    ) {
        previousBackend.stopActiveTunnels()
        // stop lockdown if we switch from that mode
        if (previousAppMode == AppMode.LOCK_DOWN)
            proxyUserspaceTunnel.setBackendMode(BackendMode.Inactive)
    }

    // TODO refactor this
    private suspend fun handleStateRestore() {
        val settings = settingsRepository.flow.first()
        val autoTunnelSettings = autoTunnelSettingsRepository.flow.first()
        if (settings.isRestoreOnBootEnabled) {
            if (autoTunnelSettings.isAutoTunnelEnabled) {
                tunnelsRepository.resetActiveTunnels()
                return autoTunnelSettingsRepository.updateAutoTunnelEnabled(true)
            }
            val tunnels = tunnelsRepository.flow.first()
            when (settings.appMode) {
                // TODO eventually, lockdown/proxy can support multi
                AppMode.VPN,
                AppMode.LOCK_DOWN,
                AppMode.PROXY ->
                    tunnels
                        .firstOrNull { it.isActive }
                        ?.let {
                            // clear any duplicates
                            tunnelsRepository.resetActiveTunnels()
                            startTunnel(it)
                        }
                // kernel supports multi
                AppMode.KERNEL ->
                    tunnels.filter { it.isActive }.forEach { conf -> startTunnel(conf) }
            }
        }
    }

    private suspend fun handleTunnelMonitoringChanges(
        activeTuns: Map<Int, TunnelState>,
        configs: List<TunnelConfig>,
    ) {
        configs
            .filter { it.restartOnPingFailure && activeTuns.keys.contains(it.id) }
            .forEach { conf ->
                val tunState = activeTuns[conf.id] ?: return@forEach
                if (tunState.health() == TunnelState.Health.UNHEALTHY) {
                    runCatching {
                            val updated = handleDnsReresolve(conf)
                            // TODO user messages
                            if (updated) {
                                Timber.i("Successfully update the peer endpoint to new address.")
                            } else {
                                Timber.i("Current endpoint address is already up to date.")
                            }
                        }
                        .onFailure {
                            Timber.e(it, "Failed to handle dns re-resolution for ${conf.name}")
                        }
                    // TODO backoff
                    delay(30_000L)
                }
            }
    }

    private suspend fun handleTunnelsActiveChange(
        previousActiveTuns: Map<Int, TunnelState>,
        activeTuns: Map<Int, TunnelState>,
        tuns: List<TunnelConfig>,
    ) {
        val relevantTunnels = previousActiveTuns.keys + activeTuns.keys

        relevantTunnels.forEach { tunnelId ->
            val wasActive = previousActiveTuns.containsKey(tunnelId)
            val isActiveNow = activeTuns.containsKey(tunnelId)

            when {
                !wasActive && isActiveNow -> {
                    tuns
                        .find { it.id == tunnelId }
                        ?.let { dbTunnelConf ->
                            tunnelsRepository.save(dbTunnelConf.copy(isActive = true))
                        }
                }
                wasActive && !isActiveNow -> {
                    tuns
                        .find { it.id == tunnelId }
                        ?.let { dbTunnelConf ->
                            tunnelsRepository.save(dbTunnelConf.copy(isActive = false))
                        }
                }
            }
        }
    }

    private suspend fun handleFullTunnelMonitoring(
        activeTuns: Map<Int, TunnelState>,
        configs: List<TunnelConfig>,
        settings: GeneralSettings,
    ) =
        monitoringMutex.withLock {
            val activeIds = activeTuns.keys.toSet()
            val currentJobs = monitoringJobs.keys.toSet()
            val obsoleteIds = currentJobs - activeIds

            Timber.d(
                "Monitoring: Active IDs: $activeIds, Obsolete IDs: $obsoleteIds, Total jobs before: ${monitoringJobs.size}"
            )

            obsoleteIds.forEach { id ->
                monitoringJobs[id]?.cancel()
                monitoringJobs.remove(id)
            }

            activeIds.forEach { id ->
                if (monitoringJobs.containsKey(id)) return@forEach // Skip if already monitored
                configs.find { it.id == id } ?: return@forEach
                val tunStateFlow =
                    activeTunnels.map { it[id] }.stateIn(applicationScope + ioDispatcher)
                val newJob =
                    applicationScope.launch(ioDispatcher) {
                        tunnelMonitor.startMonitoring(
                            id,
                            withLogs = settings.appMode != AppMode.KERNEL,
                            tunStateFlow = tunStateFlow,
                            getStatistics = { tunnelId -> getStatistics(tunnelId) },
                            updateTunnelStatus = { tid, status, stats, pings, logHealth ->
                                updateTunnelStatus(tid, null, stats, pings, logHealth)
                            },
                        )
                    }
                monitoringJobs[id] = newJob
            }
        }
}
