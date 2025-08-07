package com.zaneschepke.wireguardautotunnel.core.service.autotunnel

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.zaneschepke.networkmonitor.AndroidNetworkMonitor
import com.zaneschepke.networkmonitor.ConnectivityState
import com.zaneschepke.networkmonitor.NetworkMonitor
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.core.notification.NotificationManager
import com.zaneschepke.wireguardautotunnel.core.notification.WireGuardNotification
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelMonitor
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendState
import com.zaneschepke.wireguardautotunnel.domain.enums.NotificationAction
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus.StopReason.Ping
import com.zaneschepke.wireguardautotunnel.domain.events.AutoTunnelEvent
import com.zaneschepke.wireguardautotunnel.domain.model.AppSettings
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.domain.state.AutoTunnelState
import com.zaneschepke.wireguardautotunnel.domain.state.NetworkState
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.Tunnels
import com.zaneschepke.wireguardautotunnel.util.extensions.toMillis
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider
import kotlin.math.pow

@AndroidEntryPoint
class AutoTunnelService : LifecycleService() {

    @Inject lateinit var networkMonitor: NetworkMonitor

    @Inject lateinit var appDataRepository: Provider<AppDataRepository>

    @Inject lateinit var notificationManager: NotificationManager

    @Inject @IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher

    @Inject lateinit var serviceManager: ServiceManager

    @Inject lateinit var tunnelManager: TunnelManager

    @Inject lateinit var tunnelMonitor: TunnelMonitor

    private val defaultState = AutoTunnelState()

    private val autoTunMutex = Mutex()

    private val autoTunnelStateFlow = MutableStateFlow(defaultState)

    private val bounceCounts = MutableStateFlow<Map<Int, Int>>(emptyMap())

    private var eventHandlerJob: Job? = null

    private val lastBounceTimes = mutableMapOf<Int, Long>()

    class LocalBinder(val service: AutoTunnelService) : Binder()

    private val binder = LocalBinder(this)

    override fun onCreate() {
        super.onCreate()
        launchWatcherNotification()
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Timber.d("onStartCommand executed with startId: $startId")
        start()
        return START_STICKY
    }

    fun start() {
        launchWatcherNotification()
        startAutoTunnelStateJob()
        startLocationPermissionsNotificationJob()
    }

    fun stop() {
        stopSelf()
    }

    override fun onDestroy() {
        serviceManager.handleAutoTunnelServiceDestroy()
        restoreVpnKillSwitch()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun restoreVpnKillSwitch() {
        with(autoTunnelStateFlow.value) {
            if (
                settings.isVpnKillSwitchEnabled &&
                    tunnelManager.getBackendState() != BackendState.KILL_SWITCH_ACTIVE
            ) {
                eventHandlerJob?.cancel()
                val allowedIps =
                    if (settings.isLanOnKillSwitchEnabled) TunnelConf.LAN_BYPASS_ALLOWED_IPS
                    else emptyList()
                tunnelManager.setBackendState(BackendState.KILL_SWITCH_ACTIVE, allowedIps)
            }
        }
    }

    private fun launchWatcherNotification(
        description: String = getString(R.string.monitoring_state_changes)
    ) {
        val notification =
            notificationManager.createNotification(
                WireGuardNotification.NotificationChannels.AUTO_TUNNEL,
                title = getString(R.string.auto_tunnel_title),
                description = description,
                actions =
                    listOf(
                        notificationManager.createNotificationAction(
                            NotificationAction.AUTO_TUNNEL_OFF
                        )
                    ),
            )
        ServiceCompat.startForeground(
            this,
            NotificationManager.AUTO_TUNNEL_NOTIFICATION_ID,
            notification,
            Constants.SYSTEM_EXEMPT_SERVICE_TYPE_ID,
        )
    }

    private fun startAutoTunnelStateJob() =
        lifecycleScope.launch(ioDispatcher) {
            val networkFlow =
                debouncedConnectivityStateFlow
                    .flowOn(ioDispatcher)
                    .map(NetworkState::from)
                    .map { StateChange.NetworkChange(it) }
                    .distinctUntilChanged()

            val settingsFlow =
                combineSettings().map { StateChange.SettingsChange(it.first, it.second) }

            val tunnelsFlow =
                tunnelManager.activeTunnels.map { StateChange.ActiveTunnelsChange(it) }

            val monitoringFlow =
                tunnelManager.activeTunnels
                    .map { map -> map.mapValues { (_, state) -> state.pingStates } }
                    .distinctUntilChanged()
                    .map { StateChange.MonitoringChange(it) }

            var reevaluationJob: Job? = null

            // get everything in sync before we use merge
            combine(networkFlow, settingsFlow, tunnelsFlow, monitoringFlow) {
                    network,
                    settings,
                    tunnels,
                    monitoring ->
                    autoTunnelStateFlow.update {
                        it.copy(
                            activeTunnels = tunnels.activeTunnels,
                            networkState = network.networkState,
                            settings = settings.settings,
                            tunnels = settings.tunnels,
                        )
                    }
                }
                .first()

            // use merge to limit the noise of a combine and also increase the scalability of auto
            // tunnel handling new states
            merge(networkFlow, settingsFlow, tunnelsFlow, monitoringFlow).collect { change ->
                if (change !is StateChange.ActiveTunnelsChange) {
                    Timber.d("New state changed to ${change.javaClass.simpleName}")
                }

                when (change) {
                    is StateChange.NetworkChange -> {
                        reevaluationJob?.cancel()
                        val previousState = autoTunnelStateFlow.value
                        autoTunnelStateFlow.update { it.copy(networkState = change.networkState) }
                        // Android late mobile data state change, we can ignore handling this
                        if (
                            isAndroidLateCellularActiveChange(
                                previousState.networkState,
                                change.networkState,
                            )
                        ) {
                            Timber.d("Android late cellular active state change")
                            return@collect
                        }
                    }
                    is StateChange.SettingsChange -> {
                        reevaluationJob?.cancel()
                        autoTunnelStateFlow.update {
                            it.copy(settings = change.settings, tunnels = change.tunnels)
                        }
                    }
                    is StateChange.ActiveTunnelsChange -> {
                        autoTunnelStateFlow.update { it.copy(activeTunnels = change.activeTunnels) }
                        return@collect
                    }
                    is StateChange.MonitoringChange -> {
                        change.pingStates.forEach { (config, pingState) ->
                            Timber.d("Ping state $pingState")
                            if (pingState?.all { it.value.isReachable } == true) {
                                Timber.d("Clearing bounce count on success")
                                bounceCounts.update { current ->
                                    current.toMutableMap().apply { remove(config.id) }
                                }
                            }
                        }
                        return@collect handleAutoTunnelEvent(
                            autoTunnelStateFlow.value.determineAutoTunnelEvent(
                                StateChange.MonitoringChange(change.pingStates)
                            )
                        )
                    }
                }

                handleAutoTunnelEvent(autoTunnelStateFlow.value.determineAutoTunnelEvent(change))

                reevaluationJob = launch {
                    delay(REEVALUATE_CHECK_DELAY)
                    val currentState = autoTunnelStateFlow.value
                    if (currentState != defaultState) {
                        Timber.d("Re-evaluating auto-tunnel state..")
                        handleAutoTunnelEvent(currentState.determineAutoTunnelEvent(change))
                    }
                }
            }
        }

    private fun isAndroidLateCellularActiveChange(
        previous: NetworkState,
        new: NetworkState,
    ): Boolean {
        return (previous.isWifiConnected != new.isWifiConnected &&
            previous.wifiName == new.wifiName &&
            previous.isMobileDataConnected != new.isMobileDataConnected)
    }

    // all relevant settings to auto tunnel
    private fun areAutoTunnelSettingsTheSame(old: AppSettings, new: AppSettings): Boolean {
        return (old.isTunnelOnWifiEnabled == new.isTunnelOnWifiEnabled &&
            old.isTunnelOnMobileDataEnabled == new.isTunnelOnMobileDataEnabled &&
            old.isTunnelOnEthernetEnabled == new.isTunnelOnEthernetEnabled &&
            old.trustedNetworkSSIDs == new.trustedNetworkSSIDs &&
            old.isPingEnabled == new.isPingEnabled &&
            old.debounceDelaySeconds == new.debounceDelaySeconds &&
            old.wifiDetectionMethod == new.wifiDetectionMethod &&
            old.isVpnKillSwitchEnabled == new.isVpnKillSwitchEnabled &&
            old.isLanOnKillSwitchEnabled == new.isLanOnKillSwitchEnabled &&
            old.isDisableKillSwitchOnTrustedEnabled == new.isDisableKillSwitchOnTrustedEnabled &&
            old.isStopOnNoInternetEnabled == new.isStopOnNoInternetEnabled)
    }

    private fun combineSettings(): Flow<Pair<AppSettings, Tunnels>> {
        return combine(
                appDataRepository
                    .get()
                    .settings
                    .flow
                    .distinctUntilChanged(::areAutoTunnelSettingsTheSame),
                appDataRepository.get().tunnels.flow.map { tunnels ->
                    // isActive is ignored for equality checks so user can manually toggle off
                    // tunnel with auto-tunnel
                    tunnels.map { it.copy(isActive = false) }
                },
            ) { settings, tunnels ->
                Pair(settings, tunnels)
            }
            .distinctUntilChanged()
    }

    private fun areAutoTunnelPermissionsRequiredTheSame(
        old: AutoTunnelState,
        new: AutoTunnelState,
    ): Boolean {
        return (old.settings.wifiDetectionMethod == new.settings.wifiDetectionMethod &&
            old.networkState.locationPermissionGranted ==
                new.networkState.locationPermissionGranted &&
            old.networkState.locationServicesEnabled == new.networkState.locationServicesEnabled &&
            old.tunnels == new.tunnels &&
            old.settings.trustedNetworkSSIDs == new.settings.trustedNetworkSSIDs)
    }

    //     watch for changes to location permission and notify user it will impact auto-tunneling
    //     TODO or a recheck button for location permission so we dont have to poll it
    private fun startLocationPermissionsNotificationJob(): Job =
        lifecycleScope.launch(ioDispatcher) {
            var locationServicesShown = false
            var locationPermissionsShown = false

            data class NetworkPermissionState(
                val detectionMethod: AndroidNetworkMonitor.WifiDetectionMethod,
                val locationServicesEnabled: Boolean,
                val locationPermissionsEnabled: Boolean,
                val ssidReadRequired: Boolean,
            )

            autoTunnelStateFlow
                .distinctUntilChanged(::areAutoTunnelPermissionsRequiredTheSame)
                .map {
                    NetworkPermissionState(
                        it.settings.wifiDetectionMethod,
                        it.networkState.locationServicesEnabled == true,
                        it.networkState.locationPermissionGranted == true,
                        (it.tunnels.any { tunnel -> tunnel.tunnelNetworks.isNotEmpty() } ||
                            it.settings.trustedNetworkSSIDs.isNotEmpty()),
                    )
                }
                .collect { state ->
                    when (state.detectionMethod) {
                        AndroidNetworkMonitor.WifiDetectionMethod.DEFAULT,
                        AndroidNetworkMonitor.WifiDetectionMethod.LEGACY -> {
                            if (
                                !state.locationPermissionsEnabled &&
                                    !locationPermissionsShown &&
                                    state.ssidReadRequired
                            ) {
                                locationPermissionsShown = true
                                val notification =
                                    notificationManager.createNotification(
                                        WireGuardNotification.NotificationChannels.AUTO_TUNNEL,
                                        title = getString(R.string.warning),
                                        description =
                                            getString(R.string.location_permissions_missing),
                                    )
                                notificationManager.show(
                                    NotificationManager.AUTO_TUNNEL_LOCATION_PERMISSION_ID,
                                    notification,
                                )
                            }
                            if (
                                !state.locationServicesEnabled &&
                                    !locationServicesShown &&
                                    state.ssidReadRequired
                            ) {
                                locationServicesShown = true
                                val notification =
                                    notificationManager.createNotification(
                                        WireGuardNotification.NotificationChannels.AUTO_TUNNEL,
                                        title = getString(R.string.warning),
                                        description =
                                            getString(R.string.location_services_not_detected),
                                    )
                                notificationManager.show(
                                    NotificationManager.AUTO_TUNNEL_LOCATION_SERVICES_ID,
                                    notification,
                                )
                            }
                            if (state.locationServicesEnabled || !state.ssidReadRequired) {
                                notificationManager.remove(
                                    NotificationManager.AUTO_TUNNEL_LOCATION_SERVICES_ID
                                )
                                locationServicesShown = false
                            }
                            if (state.locationPermissionsEnabled || !state.ssidReadRequired) {
                                notificationManager.remove(
                                    NotificationManager.AUTO_TUNNEL_LOCATION_PERMISSION_ID
                                )
                                locationPermissionsShown = false
                            }
                        }
                        else -> Unit
                    }
                }
        }

    private suspend fun handleAutoTunnelEvent(autoTunnelEvent: AutoTunnelEvent) {
        autoTunMutex.withLock {
            when (
                val event =
                    autoTunnelEvent.also {
                        Timber.i("Auto tunnel event: ${it.javaClass.simpleName}")
                    }
            ) {
                is AutoTunnelEvent.Start ->
                    (event.tunnelConf ?: appDataRepository.get().getPrimaryOrFirstTunnel())?.let {
                        tunnelManager.startTunnel(it)
                    }
                is AutoTunnelEvent.Stop -> tunnelManager.stopTunnel()
                AutoTunnelEvent.DoNothing -> Timber.i("Auto-tunneling: nothing to do")
                is AutoTunnelEvent.Bounce ->
                    handleBounceWithBackoff(event.configsPeerKeyResolvedMap)
                is AutoTunnelEvent.StartKillSwitch -> {
                    Timber.d("Starting kill switch")
                    tunnelManager.setBackendState(BackendState.KILL_SWITCH_ACTIVE, event.allowedIps)
                }
                AutoTunnelEvent.StopKillSwitch -> {
                    Timber.d("Stopping kill switch")
                    tunnelManager.setBackendState(BackendState.SERVICE_ACTIVE, emptySet())
                }
            }
        }
    }

    private suspend fun handleBounceWithBackoff(
        configsPeerKeyResolvedMap: List<Pair<TunnelConf, Map<String, String?>>>
    ) { // Simplified param: no failureCount
        val settings = appDataRepository.get().settings.get()
        val pingIntervalMillis = settings.tunnelPingIntervalSeconds.toMillis()
        configsPeerKeyResolvedMap.forEach { (config, peerMap) ->
            val bounceCount = bounceCounts.value.getOrDefault(config.id, 0)
            val exponent = bounceCount.toDouble()
            val backoffDelay =
                (pingIntervalMillis * 2.0.pow(exponent)).toLong().coerceAtMost(MAX_BACKOFF_MS)
            val currentTime = System.currentTimeMillis()
            val lastTime = lastBounceTimes.getOrDefault(config.id, 0L)
            if (currentTime - lastTime >= backoffDelay) {
                Timber.d(
                    "Bouncing tunnel ${config.name} after detecting failure, with bounce count $bounceCount and calculated backoff delay $backoffDelay ms"
                )
                tunnelManager.bounceTunnel(config, Ping(peerMap))
                lastBounceTimes[config.id] = currentTime
                bounceCounts.update { current ->
                    current.toMutableMap().apply { this[config.id] = (this[config.id] ?: 0) + 1 }
                }
            } else {
                Timber.d(
                    "Backoff in progress for tunnel ${config.name}, skipping bounce (required delay: $backoffDelay ms)"
                )
            }
        }
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val debouncedConnectivityStateFlow: Flow<ConnectivityState> by lazy {
        appDataRepository
            .get()
            .settings
            .flow
            .map { it.debounceDelaySeconds.toMillis() }
            .distinctUntilChanged()
            .flatMapLatest { debounceMillis ->
                networkMonitor.connectivityStateFlow.debounce(debounceMillis)
            }
    }

    companion object {
        // try to keep this window short as it will interrupt manual overrides
        const val REEVALUATE_CHECK_DELAY = 2_000L
        const val MAX_BACKOFF_MS = 300_000L // 5 minutes
    }
}
