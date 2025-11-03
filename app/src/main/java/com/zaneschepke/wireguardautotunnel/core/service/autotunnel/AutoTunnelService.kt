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

    private val defaultState = AutoTunnelState()

    private val autoTunMutex = Mutex()

    private val autoTunnelStateFlow = MutableStateFlow(defaultState)

    class LocalBinder(service: AutoTunnelService) : Binder() {
        private val serviceRef = WeakReference(service)

        val service: AutoTunnelService?
            get() = serviceRef.get()
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
        networkMonitor.destroy()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
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
                onGoing = true,
                groupKey = NotificationManager.AUTO_TUNNEL_GROUP_KEY,
                isGroupSummary = true,
            )
        ServiceCompat.startForeground(
            this,
            NotificationManager.AUTO_TUNNEL_NOTIFICATION_ID,
            notification,
            Constants.SPECIAL_USE_SERVICE_TYPE_ID,
        )
    }

    private fun startAutoTunnelStateJob() =
        lifecycleScope.launch(ioDispatcher) {
            val networkFlow =
                debouncedConnectivityStateFlow
                    .flowOn(ioDispatcher)
                    .map { it.toDomain() }
                    .map(::NetworkChange)
                    .distinctUntilChanged()

            val settingsFlow =
                combineSettings().map { (appMode, settings, tunnels) ->
                    SettingsChange(appMode, settings, tunnels)
                }

            val tunnelsFlow = tunnelManager.activeTunnels.map(::ActiveTunnelsChange)

            var reevaluationJob: Job? = null

            // get everything in sync before we use merge
            combine(networkFlow, settingsFlow, tunnelsFlow) { network, settings, tunnels ->
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

            val initialState = autoTunnelStateFlow.value
            if (initialState != defaultState) {
                handleAutoTunnelEvent(
                    initialState.determineAutoTunnelEvent(NetworkChange(initialState.networkState))
                )
            }

            // use merge to limit the noise of a combine and also increase the scalability of auto
            // tunnel handling new states
            merge(networkFlow, settingsFlow, tunnelsFlow).collect { change ->
                if (change !is ActiveTunnelsChange) {
                    Timber.d("New state changed to ${change.javaClass.simpleName}")
                }

                val previousState = autoTunnelStateFlow.value

                when (change) {
                    is NetworkChange -> {
                        Timber.d("Network change: ${change.networkState}")
                        reevaluationJob?.cancel()
                        autoTunnelStateFlow.update { it.copy(networkState = change.networkState) }
                        if (previousState.networkState == change.networkState) {
                            Timber.d("Duplicate network state change detected, ignoring")
                            return@collect
                        }
                    }
                    is SettingsChange -> {
                        reevaluationJob?.cancel()
                        autoTunnelStateFlow.update {
                            it.copy(settings = change.settings, tunnels = change.tunnels)
                        }
                        if (
                            previousState.settings == change.settings &&
                                previousState.tunnels == change.tunnels
                        ) {
                            Timber.d("Duplicate settings change detected, ignoring")
                            return@collect
                        }
                    }
                    is ActiveTunnelsChange -> {
                        autoTunnelStateFlow.update { it.copy(activeTunnels = change.activeTunnels) }
                        return@collect
                    }
                }

                handleAutoTunnelEvent(autoTunnelStateFlow.value.determineAutoTunnelEvent(change))

                reevaluationJob = launch {
                    val snapshotNetwork = autoTunnelStateFlow.value.networkState
                    delay(REEVALUATE_CHECK_DELAY)
                    val currentState = autoTunnelStateFlow.value
                    if (
                        currentState != defaultState && currentState.networkState != snapshotNetwork
                    ) {
                        Timber.d(
                            "Re-evaluating auto-tunnel state.. (network changed since snapshot)"
                        )
                        handleAutoTunnelEvent(currentState.determineAutoTunnelEvent(change))
                    } else {
                        Timber.d("Skipping re-eval: network unchanged or default state")
                    }
                }
            }
        }

    private fun combineSettings(): Flow<Triple<AppMode, AutoTunnelSettings, List<TunnelConfig>>> {
        return combine(
                settingsRepository.flow.map { it.appMode }.distinctUntilChanged(),
                autoTunnelRepository.get().flow,
                tunnelsRepository.flow.map { tunnels ->
                    // isActive is ignored for equality checks so user can manually toggle off
                    // tunnel with auto-tunnel
                    tunnels.map { it.copy(isActive = false) }
                },
            ) { appMode, autoTunnel, tunnels ->
                Triple(appMode, autoTunnel, tunnels)
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
                    (event.tunnelConfig ?: tunnelsRepository.getDefaultTunnel())?.let {
                        tunnelManager.startTunnel(it)
                    }
                is AutoTunnelEvent.Stop -> tunnelManager.stopActiveTunnels()
                AutoTunnelEvent.DoNothing -> Timber.i("Auto-tunneling: nothing to do")
            }
        }
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val debouncedConnectivityStateFlow: Flow<ConnectivityState> by lazy {
        autoTunnelRepository
            .get()
            .flow
            .map { it.debounceDelaySeconds.toMillis() }
            .distinctUntilChanged()
            .flatMapLatest { debounceMillis ->
                networkMonitor.connectivityStateFlow.debounce(debounceMillis)
            }
    }

    companion object {
        const val REEVALUATE_CHECK_DELAY = 3_000L
    }
}
