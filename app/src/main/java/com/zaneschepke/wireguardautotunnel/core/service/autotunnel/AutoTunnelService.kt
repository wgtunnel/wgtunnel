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
import com.zaneschepke.wireguardautotunnel.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.enums.NotificationAction
import com.zaneschepke.wireguardautotunnel.domain.events.AutoTunnelEvent
import com.zaneschepke.wireguardautotunnel.domain.model.AutoTunnelSettings
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.domain.repository.AutoTunnelSettingsRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.GeneralSettingRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.domain.state.AutoTunnelState
import com.zaneschepke.wireguardautotunnel.domain.state.toDomain
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.to
import com.zaneschepke.wireguardautotunnel.util.extensions.toMillis
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import com.zaneschepke.wireguardautotunnel.domain.state.ActiveNetwork
import com.zaneschepke.wireguardautotunnel.core.service.autotunnel.handler.AutoTunnelRoamingHandler
import com.zaneschepke.wireguardautotunnel.util.extensions.isMatchingToWildcardList

@AndroidEntryPoint
class AutoTunnelService : LifecycleService() {

    @Inject lateinit var networkMonitor: NetworkMonitor
    @Inject lateinit var notificationManager: NotificationManager
    @Inject @IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher
    @Inject lateinit var serviceManager: ServiceManager
    @Inject lateinit var tunnelManager: TunnelManager
    @Inject lateinit var autoTunnelRepository: Provider<AutoTunnelSettingsRepository>
    @Inject lateinit var settingsRepository: GeneralSettingRepository
    @Inject lateinit var tunnelsRepository: TunnelRepository
    
    @Inject lateinit var roamingHandler: AutoTunnelRoamingHandler

    private val defaultState = AutoTunnelState()
    private val autoTunMutex = Mutex()
    private val autoTunnelStateFlow = MutableStateFlow(defaultState)
    private var autoTunnelJob: Job? = null
    private var permissionsJob: Job? = null

    class LocalBinder(service: AutoTunnelService) : Binder() {
        private val serviceRef = WeakReference(service)
        val service: AutoTunnelService? get() = serviceRef.get()
    }

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
        start()
        return START_STICKY
    }

    fun start() {
        launchWatcherNotification()
        autoTunnelJob?.cancel()
        autoTunnelJob = startAutoTunnelStateJob()
        permissionsJob?.cancel()
        permissionsJob = startLocationPermissionsNotificationJob()
    }

    fun stop() {
        stopSelf()
    }

    override fun onDestroy() {
        roamingHandler.cleanup()
        serviceManager.handleAutoTunnelServiceDestroy()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun launchWatcherNotification(description: String = getString(R.string.monitoring_state_changes)) {
        val notification = notificationManager.createNotification(
            WireGuardNotification.NotificationChannels.AUTO_TUNNEL,
            title = getString(R.string.auto_tunnel_title),
            description = description,
            actions = listOf(notificationManager.createNotificationAction(NotificationAction.AUTO_TUNNEL_OFF)),
            onGoing = true,
            groupKey = NotificationManager.AUTO_TUNNEL_GROUP_KEY,
            isGroupSummary = true,
        )
        ServiceCompat.startForeground(this, NotificationManager.AUTO_TUNNEL_NOTIFICATION_ID, notification, Constants.SPECIAL_USE_SERVICE_TYPE_ID)
    }

    private fun startAutoTunnelStateJob(): Job = lifecycleScope.launch(ioDispatcher) {
        // We use the dynamic debounced flow here
        val networkFlow = debouncedConnectivityStateFlow.flowOn(ioDispatcher).map { it.toDomain() }.map(::NetworkChange).distinctUntilChanged()
        val settingsFlow = combineSettings().map { (appMode, settings, tunnels) -> SettingsChange(appMode, settings, tunnels) }
        val tunnelsFlow = tunnelManager.activeTunnels.map(::ActiveTunnelsChange)
        var reevaluationJob: Job? = null

        combine(networkFlow, settingsFlow, tunnelsFlow) { network, settings, tunnels ->
            autoTunnelStateFlow.update {
                it.copy(activeTunnels = tunnels.activeTunnels, networkState = network.networkState, settings = settings.settings, tunnels = settings.tunnels)
            }
        }.first()

        val initialState = autoTunnelStateFlow.value
        if (initialState != defaultState) {
            handleAutoTunnelEvent(initialState.determineAutoTunnelEvent(NetworkChange(initialState.networkState)))
        }

        merge(networkFlow, settingsFlow, tunnelsFlow).collect { change ->
            val previousState = autoTunnelStateFlow.value

            // --- AUTO-SAVE LOGIC ---
            val settings = previousState.settings
            if (change is NetworkChange && settings.isBssidRoamingEnabled && settings.isBssidAutoSaveEnabled) {
                val oldNet = previousState.networkState.activeNetwork
                val newNet = change.networkState.activeNetwork
                
                if (oldNet is ActiveNetwork.Wifi && newNet is ActiveNetwork.Wifi) {
                    val isOldValid = !oldNet.bssid.isNullOrBlank() && oldNet.bssid != "02:00:00:00:00:00" && oldNet.bssid != "00:00:00:00:00:00"
                    val isNewValid = !newNet.bssid.isNullOrBlank() && newNet.bssid != "02:00:00:00:00:00" && newNet.bssid != "00:00:00:00:00:00"

                    if (oldNet.ssid == newNet.ssid && oldNet.bssid != newNet.bssid && isOldValid && isNewValid) {
                        
                        val alreadyCovered = if (settings.isBssidWildcardsEnabled) {
                            settings.roamingSSIDs.isMatchingToWildcardList(newNet.ssid)
                        } else {
                            settings.roamingSSIDs.contains(newNet.ssid)
                        }

                        if (!alreadyCovered) {
                            Timber.i("Auto-save: Detected roaming on ${newNet.ssid}. Adding to list.")
                            val newSet = settings.roamingSSIDs + newNet.ssid
                            launch(ioDispatcher) {
                                autoTunnelRepository.get().upsert(settings.copy(roamingSSIDs = newSet))
                            }
                        }
                    }
                }
            }
            // -----------------------

            when (change) {
                is NetworkChange -> {
                    reevaluationJob?.cancel()
                    autoTunnelStateFlow.update { it.copy(networkState = change.networkState) }
                    if (previousState.networkState == change.networkState) return@collect
                }
                is SettingsChange -> {
                    reevaluationJob?.cancel()
                    autoTunnelStateFlow.update { it.copy(settings = change.settings, tunnels = change.tunnels) }
                    if (previousState.settings == change.settings && previousState.tunnels == change.tunnels) return@collect
                }
                is ActiveTunnelsChange -> {
                    autoTunnelStateFlow.update { it.copy(activeTunnels = change.activeTunnels) }
                    return@collect
                }
            }

            val currentState = autoTunnelStateFlow.value
            val event = currentState.determineAutoTunnelEvent(change, previousState)
            
            // Delegate Restart (Roaming) to Handler, Handle others normally
            if (event is AutoTunnelEvent.Restart) {
                Timber.i("Service: Roaming detected via Dynamic Debounce. Delegating to Handler.")
                roamingHandler.onRoamingDetected(event.tunnelConfig)
            } else {
                handleAutoTunnelEvent(event)
            }

            reevaluationJob = launch {
                val snapshotNetwork = autoTunnelStateFlow.value.networkState
                delay(REEVALUATE_CHECK_DELAY)
                val delayedState = autoTunnelStateFlow.value
                if (delayedState != defaultState && delayedState.networkState != snapshotNetwork) {
                     val delayedEvent = delayedState.determineAutoTunnelEvent(change, previousState)
                     if (delayedEvent is AutoTunnelEvent.Restart) {
                        Timber.i("Service: Roaming detected (Delayed). Delegating to Handler.")
                        roamingHandler.onRoamingDetected(delayedEvent.tunnelConfig)
                     } else {
                        handleAutoTunnelEvent(delayedEvent)
                     }
                }
            }
        }
    }

    private fun combineSettings(): Flow<Triple<AppMode, AutoTunnelSettings, List<TunnelConfig>>> {
        return combine(
                settingsRepository.flow.map { it.appMode }.distinctUntilChanged(),
                autoTunnelRepository.get().flow,
                tunnelsRepository.userTunnelsFlow.map { tunnels -> tunnels.map { it.copy(isActive = false) } },
            ) { appMode, autoTunnel, tunnels -> Triple(appMode, autoTunnel, tunnels) }
            .distinctUntilChanged()
    }

    private fun areAutoTunnelPermissionsRequiredTheSame(old: AutoTunnelState, new: AutoTunnelState): Boolean {
        return (old.settings.wifiDetectionMethod == new.settings.wifiDetectionMethod &&
            old.networkState.locationPermissionGranted == new.networkState.locationPermissionGranted &&
            old.networkState.locationServicesEnabled == new.networkState.locationServicesEnabled &&
            old.tunnels == new.tunnels &&
            old.settings.trustedNetworkSSIDs == new.settings.trustedNetworkSSIDs)
    }

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
                        it.settings.wifiDetectionMethod.to(),
                        it.networkState.locationServicesEnabled,
                        it.networkState.locationPermissionGranted,
                        (it.tunnels.any { tunnel -> tunnel.tunnelNetworks.isNotEmpty() } ||
                            it.settings.trustedNetworkSSIDs.isNotEmpty()),
                    )
                }
                .collect { state ->
                    when (state.detectionMethod) {
                        AndroidNetworkMonitor.WifiDetectionMethod.DEFAULT,
                        AndroidNetworkMonitor.WifiDetectionMethod.LEGACY -> {
                            if (!state.locationPermissionsEnabled && !locationPermissionsShown && state.ssidReadRequired) {
                                locationPermissionsShown = true
                                notificationManager.show(
                                    NotificationManager.AUTO_TUNNEL_LOCATION_PERMISSION_ID,
                                    notificationManager.createNotification(
                                        WireGuardNotification.NotificationChannels.AUTO_TUNNEL,
                                        title = getString(R.string.warning),
                                        description = getString(R.string.location_permissions_missing),
                                    )
                                )
                            }
                            if (!state.locationServicesEnabled && !locationServicesShown && state.ssidReadRequired) {
                                locationServicesShown = true
                                notificationManager.show(
                                    NotificationManager.AUTO_TUNNEL_LOCATION_SERVICES_ID,
                                    notificationManager.createNotification(
                                        WireGuardNotification.NotificationChannels.AUTO_TUNNEL,
                                        title = getString(R.string.warning),
                                        description = getString(R.string.location_services_not_detected),
                                    )
                                )
                            }
                            if (state.locationServicesEnabled || !state.ssidReadRequired) {
                                notificationManager.remove(NotificationManager.AUTO_TUNNEL_LOCATION_SERVICES_ID)
                                locationServicesShown = false
                            }
                            if (state.locationPermissionsEnabled || !state.ssidReadRequired) {
                                notificationManager.remove(NotificationManager.AUTO_TUNNEL_LOCATION_PERMISSION_ID)
                                locationPermissionsShown = false
                            }
                        }
                        else -> Unit
                    }
                }
        }

    private suspend fun handleAutoTunnelEvent(autoTunnelEvent: AutoTunnelEvent) {
        autoTunMutex.withLock {
            when (val event = autoTunnelEvent.also { Timber.i("Auto tunnel event: ${it.javaClass.simpleName}") }) {
                
                is AutoTunnelEvent.Start -> {
                    (event.tunnelConfig ?: tunnelsRepository.getDefaultTunnel())?.let { 
                        tunnelManager.startTunnel(it) 
                    }
                }
                
                is AutoTunnelEvent.Stop -> tunnelManager.stopActiveTunnels()
                
                // Restart is handled by the delegation flow above
                is AutoTunnelEvent.Restart -> { }
                
                AutoTunnelEvent.DoNothing -> Timber.i("Auto-tunneling: nothing to do")
            }
        }
    }

    // --- DYNAMIC DEBOUNCE LOGIC ---
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val debouncedConnectivityStateFlow: Flow<ConnectivityState> by lazy {
        // 1. Combine Network State + User Delay Settings
        combine(
            networkMonitor.connectivityStateFlow,
            autoTunnelRepository.get().flow.map { it.debounceDelaySeconds.toMillis() }.distinctUntilChanged()
        ) { networkState, delay ->
            Pair(networkState, delay)
        }
        // 2. Scan to keep track of PREVIOUS state (to detect BSSID change)
        .scan(Triple<ConnectivityState?, ConnectivityState?, Long>(null, null, 0L)) { previous, current ->
             // Triple(OldState, NewState, UserDelay)
             Triple(previous.second, current.first, current.second)
        }
        .drop(1) // Skip initial seed
        // 3. Dynamic Debounce Operator
        .debounce { (oldState, newState, userDelay) ->
            if (oldState == null || newState == null) return@debounce 0L
            
            val oldNet = oldState.activeNetwork
            val newNet = newState.activeNetwork
            
            // Check for Roaming (Same SSID, Different BSSID, Both Wifi)
            val isRoaming = oldNet is ActiveNetwork.Wifi && 
                            newNet is ActiveNetwork.Wifi && 
                            oldNet.ssid == newNet.ssid && 
                            oldNet.bssid != newNet.bssid &&
                            !newNet.bssid.isNullOrBlank()

            if (isRoaming) {
                Timber.d("Dynamic Debounce: Roaming detected. Using FAST delay (250ms).")
                250L // Fast roaming
            } else {
                // Standard change (4G / New Wifi / Loss) -> Use User Delay
                userDelay 
            }
        }
        .mapNotNull { it.second } // Extract the new ConnectivityState
        .distinctUntilChanged()
    }

    companion object {
        const val REEVALUATE_CHECK_DELAY = 3_000L
    }
}
