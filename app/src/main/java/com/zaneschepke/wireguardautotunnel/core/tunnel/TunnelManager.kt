package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.di.*
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendMode
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.events.BackendCoreException
import com.zaneschepke.wireguardautotunnel.domain.events.BackendMessage
import com.zaneschepke.wireguardautotunnel.domain.events.NotAuthorized
import com.zaneschepke.wireguardautotunnel.domain.model.AutoTunnelSettings
import com.zaneschepke.wireguardautotunnel.domain.model.GeneralSettings
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.domain.repository.AutoTunnelSettingsRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.GeneralSettingRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.LockdownSettingsRepository
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
    private val lockdownSettingsRepository: LockdownSettingsRepository,
    private val tunnelsRepository: TunnelRepository,
    private val tunnelMonitor: TunnelMonitor,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TunnelProvider {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val localErrorEvents = MutableSharedFlow<Pair<String?, BackendCoreException>>()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val localMessageEvents = MutableSharedFlow<Pair<String?, BackendMessage>>()

    private val monitoringMutex = Mutex()
    private val monitoringJobs = ConcurrentHashMap<Int, Job>()

    private val ddnsMutex = Mutex()
    private val ddnsJobs = ConcurrentHashMap<Int, Job>()

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

    private val tunnelProviderFlow: StateFlow<TunnelProvider> = run {
        val currentBackend = AtomicReference(userspaceTunnel)
        val currentSettings = AtomicReference(GeneralSettings())
        val initialEmit = AtomicBoolean(true)

        settingsRepository.flow
            .filterNotNull()
            // ignore default state
            .filterNot { it == GeneralSettings() }
            .distinctUntilChangedBy { it.appMode }
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
                    handleLockDownModeInit()
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
            .onStart { handleRestore() }
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
                                SideEffectWithCondition(
                                    effect = { s ->
                                        handleDynamicDnsMonitoring(s.activeTuns, s.tuns, s.settings)
                                    },
                                    condition = { s ->
                                        s.activeTuns.keys != s.previouslyActive.keys
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
    override val errorEvents: SharedFlow<Pair<String?, BackendCoreException>> =
        merge(localErrorEvents, tunnelProviderFlow.flatMapLatest { it.errorEvents })
            .shareIn(
                scope = applicationScope + ioDispatcher,
                started = SharingStarted.Eagerly,
                replay = 0,
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    override val messageEvents: SharedFlow<Pair<String?, BackendMessage>> =
        merge(localMessageEvents, tunnelProviderFlow.flatMapLatest { it.messageEvents })
            .shareIn(
                scope = applicationScope.plus(ioDispatcher),
                started = SharingStarted.Eagerly,
                replay = 0,
            )

    override fun getStatistics(tunnelId: Int): TunnelStatistics? {
        return tunnelProviderFlow.value.getStatistics(tunnelId)
    }

    override suspend fun startTunnel(tunnelConfig: TunnelConfig) {
        if (activeTunnels.value.containsKey(tunnelConfig.id)) return
        val provider = tunnelProviderFlow.value
        val isKernel = provider is KernelTunnel

        if (!isKernel && activeTunnels.value.isNotEmpty()) {
            stopActiveTunnels()
            withTimeoutOrNull(BaseTunnel.STARTUP_TIMEOUT_MS) {
                activeTunnels.first { it.isEmpty() }
            } ?: run { activeTunnels.value.keys.forEach { id -> provider.forceStopTunnel(id) } }
        }
        tunnelProviderFlow.value.startTunnel(tunnelConfig)
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

    // TODO this can crash if we haven't started foreground service yet, especially for
    // workerManager
    private suspend fun handleLockDownModeInit() {
        val lockdownSettings = lockdownSettingsRepository.getLockdownSettings()
        val allowedIps =
            if (lockdownSettings.bypassLan) TunnelConfig.IPV4_PUBLIC_NETWORKS else emptySet()
        try {
            if (serviceManager.hasVpnPermission()) {
                proxyUserspaceTunnel.setBackendMode(
                    BackendMode.KillSwitch(
                        allowedIps,
                        lockdownSettings.metered,
                        lockdownSettings.dualStack,
                    )
                )
            } else {
                throw NotAuthorized()
            }
        } catch (e: BackendCoreException) {
            localErrorEvents.tryEmit(null to e)
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

    private fun isVpnAuthorized(
        mode: AppMode,
        hasVpnPermission: () -> Boolean = { serviceManager.hasVpnPermission() },
    ): Boolean {
        return when (mode) {
            AppMode.VPN,
            AppMode.LOCK_DOWN -> hasVpnPermission()
            AppMode.KERNEL,
            AppMode.PROXY -> true
        }
    }

    suspend fun handleRestore() =
        withContext(ioDispatcher) {
            val settings = settingsRepository.getGeneralSettings()
            val autoTunnelSettings = autoTunnelSettingsRepository.getAutoTunnelSettings()
            val tunnels = tunnelsRepository.getAll()
            if (autoTunnelSettings.isAutoTunnelEnabled)
                return@withContext restoreAutoTunnel(autoTunnelSettings)
            if (isVpnAuthorized(settings.appMode)) {
                when (val mode = settings.appMode) {
                    AppMode.VPN,
                    AppMode.PROXY,
                    AppMode.LOCK_DOWN -> {
                        if (mode == AppMode.LOCK_DOWN) handleLockDownModeInit()
                        tunnels.firstOrNull { it.isActive }?.let { startTunnel(it) }
                    }
                    AppMode.KERNEL ->
                        tunnels.filter { it.isActive }.forEach { conf -> startTunnel(conf) }
                }
            } else {
                localErrorEvents.emit(null to NotAuthorized())
            }
        }

    private suspend fun restoreAutoTunnel(autoTunnelSettings: AutoTunnelSettings) {
        autoTunnelSettingsRepository.upsert(autoTunnelSettings.copy(isAutoTunnelEnabled = true))
        serviceManager.startAutoTunnelService()
    }

    suspend fun handleReboot() =
        withContext(ioDispatcher) {
            val settings = settingsRepository.getGeneralSettings()
            val autoTunnelSettings = autoTunnelSettingsRepository.getAutoTunnelSettings()
            val defaultTunnel = tunnelsRepository.getStartTunnel()
            if (autoTunnelSettings.startOnBoot)
                return@withContext restoreAutoTunnel(autoTunnelSettings)
            if (settings.isRestoreOnBootEnabled) {
                tunnelsRepository.resetActiveTunnels()
                if (isVpnAuthorized(settings.appMode)) {
                    when (val mode = settings.appMode) {
                        AppMode.LOCK_DOWN -> handleLockDownModeInit()
                        AppMode.KERNEL,
                        AppMode.VPN,
                        AppMode.PROXY -> Unit
                    }
                    defaultTunnel?.let { startTunnel(it) }
                } else {
                    localErrorEvents.emit(null to NotAuthorized())
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

    private suspend fun handleDynamicDnsMonitoring(
        activeTuns: Map<Int, TunnelState>,
        configs: List<TunnelConfig>,
        settings: GeneralSettings,
    ) =
        ddnsMutex.withLock {
            val activeIds =
                activeTuns.keys
                    .filter { id ->
                        configs.find { it.id == id }?.restartOnPingFailure == true &&
                            settings.appMode != AppMode.KERNEL
                    }
                    .toSet()

            val currentJobs = ddnsJobs.keys.toSet()
            val obsoleteIds = currentJobs - activeIds

            Timber.d(
                "DDNS Monitoring: Active IDs: $activeIds, Obsolete IDs: $obsoleteIds, Total jobs before: ${ddnsJobs.size}"
            )

            obsoleteIds.forEach { id ->
                ddnsJobs[id]?.cancel()
                ddnsJobs.remove(id)
            }

            activeIds.forEach { id ->
                if (ddnsJobs.containsKey(id)) return@forEach // Skip if already monitored
                val conf = configs.find { it.id == id } ?: return@forEach
                val tunStateFlow =
                    activeTunnels.map { it[id] }.stateIn(applicationScope + ioDispatcher)

                val newJob =
                    applicationScope.launch(ioDispatcher) {
                        var backoff = 30_000L
                        while (isActive) {
                            val state = tunStateFlow.value ?: break
                            if (state.health() != TunnelState.Health.UNHEALTHY) {
                                backoff = BASE_BACKOFF
                                tunStateFlow.first {
                                    it?.health() == TunnelState.Health.UNHEALTHY || it == null
                                }
                                continue
                            }

                            runCatching {
                                    val updated = handleDnsReresolve(conf)
                                    if (updated) {
                                        localMessageEvents.emit(
                                            conf.name to BackendMessage.DynamicDnsSuccess
                                        )
                                        backoff = BASE_BACKOFF
                                    } else {
                                        Timber.i(
                                            "Dynamic DNS check completed, current endpoint address is already up to date."
                                        )
                                    }
                                }
                                .onFailure {
                                    Timber.e(
                                        it,
                                        "Failed to handle dns re-resolution for ${conf.name}",
                                    )
                                }

                            delay(backoff)
                            backoff = (backoff * 1.5).toLong().coerceAtMost(MAX_BACKOFF_TIME)
                        }
                    }
                ddnsJobs[id] = newJob
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
                            updateTunnelStatus = { tid, _, stats, pings, logHealth ->
                                updateTunnelStatus(tid, null, stats, pings, logHealth)
                            },
                        )
                    }
                monitoringJobs[id] = newJob
            }
        }

    companion object {
        const val BASE_BACKOFF = 30_000L
        const val MAX_BACKOFF_TIME = 300_000L
    }
}
