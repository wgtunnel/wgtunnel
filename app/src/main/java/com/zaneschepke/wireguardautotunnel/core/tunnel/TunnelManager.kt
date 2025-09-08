package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.di.*
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendMode
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.events.BackendCoreException
import com.zaneschepke.wireguardautotunnel.domain.events.BackendMessage
import com.zaneschepke.wireguardautotunnel.domain.model.GeneralSettings
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.domain.state.LogHealthState
import com.zaneschepke.wireguardautotunnel.domain.state.PingState
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelStatistics
import javax.inject.Inject
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.amnezia.awg.crypto.Key
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalAtomicApi::class)
class TunnelManager
@Inject
constructor(
    @Kernel private val kernelTunnel: TunnelProvider,
    @Userspace private val userspaceTunnel: TunnelProvider,
    @ProxyUserspace private val proxyUserspaceTunnel: TunnelProvider,
    private val serviceManager: ServiceManager,
    private val appDataRepository: AppDataRepository,
    @ApplicationScope applicationScope: CoroutineScope,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : TunnelProvider {

    private data class SideEffectState(
        val activeTuns: Map<Int, TunnelState>,
        val tuns: List<TunnelConf>,
        val settings: GeneralSettings,
        val previouslyActive: Map<Int, TunnelState>,
    )

    private data class SideEffectWithCondition(
        val effect: suspend (SideEffectState) -> Unit,
        val condition: (SideEffectState) -> Boolean,
    )

    private val sideEffectChannelFlow =
        MutableStateFlow<Channel<SideEffectState>>(Channel(Channel.CONFLATED))

    private val tunnelProviderFlow: StateFlow<TunnelProvider> = run {
        val currentBackend = AtomicReference(userspaceTunnel)
        val currentSettings = AtomicReference(GeneralSettings())
        val initialEmit = AtomicBoolean(true)

        appDataRepository.settings.flow
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
                // Create a new channel for each backend to reset side-effect processing
                val newChannel = Channel<SideEffectState>(Channel.CONFLATED)
                sideEffectChannelFlow.value = newChannel

                val sideEffects =
                    listOf(
                        SideEffectWithCondition(
                            effect = { s -> handleTunnelServiceChange(s.activeTuns) },
                            condition = { s -> s.activeTuns.size != s.previouslyActive.size },
                        ),
                        SideEffectWithCondition(
                            effect = { s ->
                                handleActiveTunnelsChange(s.previouslyActive, s.activeTuns, s.tuns)
                            },
                            condition = { s -> s.activeTuns.size != s.previouslyActive.size },
                        ),
                        // TODO Not for kernel mode for now
                        SideEffectWithCondition(
                            effect = { s -> handleTunnelMonitoringChanges(s.activeTuns, s.tuns) },
                            condition = { s ->
                                s.tuns.any {
                                    it.restartOnPingFailure && s.activeTuns.keys.contains(it.id)
                                } && s.settings.appMode != AppMode.KERNEL
                            },
                        ),
                    )

                applicationScope.launch(ioDispatcher) {
                    for (state in newChannel) {
                        supervisorScope {
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

                combine(
                    backend.activeTunnels,
                    appDataRepository.tunnels.flow,
                    appDataRepository.settings.flow.filterNotNull(),
                ) { activeTuns, tuns, settings ->
                    Triple(activeTuns, tuns, settings)
                }
            }
            .onStart { handleStateRestore() }
            .onEach { (activeTuns, tuns, settings) ->
                val previouslyActive = activeTunsReference.exchange(activeTuns)
                sideEffectChannelFlow.value.trySend(
                    SideEffectState(activeTuns, tuns, settings, previouslyActive)
                )
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

    override fun hasVpnPermission(): Boolean {
        return userspaceTunnel.hasVpnPermission()
    }

    override fun getStatistics(tunnelId: Int): TunnelStatistics? {
        return tunnelProviderFlow.value.getStatistics(tunnelId)
    }

    override suspend fun startTunnel(tunnelConf: TunnelConf) {
        // for VPN Mode, we need to stop active tunnels as we can only have one active at a time
        if (activeTunnels.value.isNotEmpty() && tunnelProviderFlow.value == userspaceTunnel)
            stopActiveTunnels()
        tunnelProviderFlow.value.startTunnel(tunnelConf)
    }

    override suspend fun stopTunnel(tunnelId: Int) {
        tunnelProviderFlow.value.stopTunnel(tunnelId)
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

    override fun handleDnsReresolve(tunnelConf: TunnelConf): Boolean {
        return tunnelProviderFlow.value.handleDnsReresolve(tunnelConf)
    }

    override suspend fun updateTunnelStatus(
        tunnelId: Int,
        status: TunnelStatus?,
        stats: TunnelStatistics?,
        pingStates: Map<Key, PingState>?,
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

    private suspend fun handleTunnelServiceChange(activeTuns: Map<Int, TunnelState>) {
        if (activeTuns.isEmpty()) serviceManager.stopTunnelForegroundService()
        if (activeTuns.isNotEmpty() && serviceManager.tunnelService.value == null)
            serviceManager.startTunnelForegroundService()
    }

    private fun handleLockDownModeInit(withLanBypass: Boolean) {
        // kill switch will always catch all ipv6, just add ipv4 networks for allowsIps
        val allowedIps = if (withLanBypass) TunnelConf.IPV4_PUBLIC_NETWORKS else emptySet()
        try {
            // TODO handle situation where they don't have vpn permission, request it
            if (hasVpnPermission()) {
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

    private suspend fun handleStateRestore() {
        val settings = appDataRepository.settings.flow.first()
        if (settings.isRestoreOnBootEnabled) {
            // if auto tun enabled, reset active and restore auto tun, letting it start appropriate
            // tuns
            if (settings.isAutoTunnelEnabled) {
                appDataRepository.tunnels.resetActiveTunnels()
                return serviceManager.startAutoTunnel()
            }
            val tunnels = appDataRepository.tunnels.flow.first()
            when (settings.appMode) {
                // TODO eventually, lockdown/proxy can support multi
                AppMode.VPN,
                AppMode.LOCK_DOWN,
                AppMode.PROXY ->
                    tunnels
                        .firstOrNull { it.isActive }
                        ?.let {
                            // clear any duplicates
                            appDataRepository.tunnels.resetActiveTunnels()
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
        configs: List<TunnelConf>,
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
                            Timber.e(it, "Failed to handle dns re-resolution for ${conf.tunName}")
                        }
                    // TODO backoff
                    delay(30_000L)
                }
            }
    }

    private suspend fun handleActiveTunnelsChange(
        previousActiveTuns: Map<Int, TunnelState>,
        activeTuns: Map<Int, TunnelState>,
        tuns: List<TunnelConf>,
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
                            appDataRepository.tunnels.save(dbTunnelConf.copy(isActive = true))
                        }
                }
                wasActive && !isActiveNow -> {
                    tuns
                        .find { it.id == tunnelId }
                        ?.let { dbTunnelConf ->
                            appDataRepository.tunnels.save(dbTunnelConf.copy(isActive = false))
                        }
                }
            }
        }
    }
}
