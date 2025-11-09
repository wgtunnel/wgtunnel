package com.zaneschepke.networkmonitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import com.wireguard.android.util.RootShell
import com.zaneschepke.networkmonitor.AndroidNetworkMonitor.WifiDetectionMethod.*
import com.zaneschepke.networkmonitor.shizuku.ShizukuShell
import com.zaneschepke.networkmonitor.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

class AndroidNetworkMonitor(
    private val appContext: Context,
    private val configurationListener: ConfigurationListener,
    private val applicationScope: CoroutineScope,
) : NetworkMonitor {

    private val actionPermissionCheck = "${appContext.packageName}.PERMISSION_CHECK"

    interface ConfigurationListener {
        val detectionMethod: Flow<WifiDetectionMethod>
        val rootShell: RootShell
    }

    companion object {
        const val LOCATION_SERVICES_FILTER: String = "android.location.PROVIDERS_CHANGED"
        const val ANDROID_UNKNOWN_SSID: String = "<unknown ssid>"
        const val SHELL_COMMAND_TIMEOUT_MS = 2_000L
    }

    enum class WifiDetectionMethod(val value: Int) {
        DEFAULT(0),
        LEGACY(1),
        ROOT(2),
        SHIZUKU(3);

        companion object {
            fun fromValue(value: Int): WifiDetectionMethod =
                entries.find { it.value == value } ?: DEFAULT
        }

        fun needsLocationPermissions(): Boolean {
            return this == DEFAULT || this == LEGACY
        }
    }

    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
    private val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
    private val locationManager =
        appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager?

    private val permissionsChangedFlow = MutableStateFlow(false)

    private var permissionReceiver: BroadcastReceiver? = null
    private var locationServicesReceiver: BroadcastReceiver? = null
    private var airplaneReceiver: BroadcastReceiver? = null
    private var defaultNetworkCallback: ConnectivityManager.NetworkCallback? = null
    private var wifiCallback: ConnectivityManager.NetworkCallback? = null
    private var cellularCallback: ConnectivityManager.NetworkCallback? = null
    private var ethernetCallback: ConnectivityManager.NetworkCallback? = null

    private val airplaneModeState = MutableStateFlow(appContext.isAirplaneModeOn())
    private val airplaneModeFlow: Flow<Boolean> = airplaneModeState.asStateFlow()

    // recreate defaultNetwork flow on permission/detection method changes to get newly available
    // network info
    @OptIn(ExperimentalCoroutinesApi::class)
    private val defaultNetworkFlow: Flow<TransportEvent> =
        combine(configurationListener.detectionMethod, permissionsChangedFlow) { detectionMethod, _
                ->
                detectionMethod
            }
            .flatMapLatest { detectionMethod ->
                callbackFlow {
                    if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && detectionMethod == DEFAULT
                    ) {
                        defaultNetworkCallback =
                            object :
                                ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
                                override fun onAvailable(network: Network) {
                                    // ignore onAvailable has it doesn't contain detailed network
                                    // information in capabilities
                                    Timber.d("Default onAvailable: $network")
                                }

                                override fun onLost(network: Network) {
                                    trySend(TransportEvent.Lost(network))
                                }

                                override fun onCapabilitiesChanged(
                                    network: Network,
                                    caps: NetworkCapabilities,
                                ) {
                                    trySend(TransportEvent.CapabilitiesChanged(network, caps))
                                }
                            }
                    } else {
                        defaultNetworkCallback =
                            object : ConnectivityManager.NetworkCallback() {
                                override fun onAvailable(network: Network) {
                                    Timber.d("Default onAvailable: $network")
                                }

                                override fun onLost(network: Network) {
                                    trySend(TransportEvent.Lost(network))
                                }

                                override fun onCapabilitiesChanged(
                                    network: Network,
                                    caps: NetworkCapabilities,
                                ) {
                                    trySend(TransportEvent.CapabilitiesChanged(network, caps))
                                }
                            }
                    }
                    connectivityManager?.registerDefaultNetworkCallback(defaultNetworkCallback!!)

                    trySend(
                        TransportEvent.Permissions(
                            Permissions(
                                locationManager?.isLocationServicesEnabled() ?: false,
                                appContext.hasRequiredLocationPermissions(),
                            )
                        )
                    )

                    awaitClose {
                        connectivityManager?.unregisterNetworkCallback(defaultNetworkCallback!!)
                    }
                }
            }

    // recreate Wi-Fi flow on permission/detection method changes to get newly available network
    // info
    @OptIn(ExperimentalCoroutinesApi::class)
    private val wifiFlow: Flow<TransportEvent> =
        combine(configurationListener.detectionMethod, permissionsChangedFlow) { detectionMethod, _
                ->
                detectionMethod
            }
            .flatMapLatest { detectionMethod -> createWifiNetworkCallbackFlow(detectionMethod) }

    private fun createWifiNetworkCallbackFlow(
        detectionMethod: WifiDetectionMethod
    ): Flow<TransportEvent> = callbackFlow {
        val onAvailable: (Network) -> Unit = { network ->
            // ignore onAvailable has it doesn't contain detailed network information in
            // capabilities
            Timber.d("WiFi onAvailable: $network")
        }
        val onLost: (Network) -> Unit = { network ->
            Timber.d("WiFi onLost: $network")
            trySend(TransportEvent.Lost(network))
        }
        val onCapabilitiesChanged: (Network, NetworkCapabilities) -> Unit = { network, caps ->
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                trySend(TransportEvent.CapabilitiesChanged(network, caps))
            }
        }

        wifiCallback =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && detectionMethod == DEFAULT) {
                object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
                    override fun onAvailable(network: Network) = onAvailable(network)

                    override fun onLost(network: Network) = onLost(network)

                    override fun onCapabilitiesChanged(
                        network: Network,
                        caps: NetworkCapabilities,
                    ) = onCapabilitiesChanged(network, caps)
                }
            } else {
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) = onAvailable(network)

                    override fun onLost(network: Network) = onLost(network)

                    override fun onCapabilitiesChanged(
                        network: Network,
                        caps: NetworkCapabilities,
                    ) = onCapabilitiesChanged(network, caps)
                }
            }

        val request =
            NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()
        connectivityManager?.registerNetworkCallback(request, wifiCallback!!)

        awaitClose {
            runCatching { connectivityManager?.unregisterNetworkCallback(wifiCallback!!) }
                .onFailure { Timber.e(it, "Error unregistering WiFi network callback") }
        }
    }

    private val cellularFlow: Flow<TransportEvent> = callbackFlow {
        val onAvailable: (Network) -> Unit = { network ->
            Timber.d("Cellular onAvailable: $network")
        }
        val onLost: (Network) -> Unit = { network ->
            Timber.d("Cellular onLost: $network")
            trySend(TransportEvent.Lost(network))
        }
        val onCapabilitiesChanged: (Network, NetworkCapabilities) -> Unit = { network, caps ->
            Timber.d("Cellular onCapabilitiesChanged: $network")
            trySend(TransportEvent.CapabilitiesChanged(network, caps))
        }

        cellularCallback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = onAvailable(network)

                override fun onLost(network: Network) = onLost(network)

                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) =
                    onCapabilitiesChanged(network, caps)
            }

        val request =
            NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build()
        connectivityManager?.registerNetworkCallback(request, cellularCallback!!)

        trySend(TransportEvent.Unknown)

        awaitClose {
            runCatching { connectivityManager?.unregisterNetworkCallback(cellularCallback!!) }
                .onFailure { Timber.e(it, "Error unregistering cellular network callback") }
        }
    }

    private val ethernetFlow: Flow<TransportEvent> = callbackFlow {
        val onAvailable: (Network) -> Unit = { network ->
            Timber.d("Ethernet onAvailable: $network")
        }
        val onLost: (Network) -> Unit = { network ->
            Timber.d("Ethernet onLost: $network")
            trySend(TransportEvent.Lost(network))
        }
        val onCapabilitiesChanged: (Network, NetworkCapabilities) -> Unit = { network, caps ->
            Timber.d("Ethernet onCapabilitiesChanged: $network")
            trySend(TransportEvent.CapabilitiesChanged(network, caps))
        }

        ethernetCallback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = onAvailable(network)

                override fun onLost(network: Network) = onLost(network)

                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) =
                    onCapabilitiesChanged(network, caps)
            }

        val request =
            NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                .build()
        connectivityManager?.registerNetworkCallback(request, ethernetCallback!!)

        trySend(TransportEvent.Unknown)

        awaitClose {
            runCatching { connectivityManager?.unregisterNetworkCallback(ethernetCallback!!) }
                .onFailure { Timber.e(it, "Error unregistering ethernet network callback") }
        }
    }

    private val wifiStateFlow: Flow<NetworkCapabilities?> =
        wifiFlow
            .map { event ->
                when (event) {
                    is TransportEvent.CapabilitiesChanged -> event.networkCapabilities
                    is TransportEvent.Lost -> null
                    else -> null
                }
            }
            .stateIn(applicationScope, SharingStarted.Eagerly, null)

    private val cellularStateFlow: Flow<NetworkCapabilities?> =
        cellularFlow
            .map { event ->
                when (event) {
                    is TransportEvent.CapabilitiesChanged -> event.networkCapabilities
                    is TransportEvent.Lost -> null
                    else -> null
                }
            }
            .stateIn(applicationScope, SharingStarted.Eagerly, null)

    private val ethernetStateFlow: Flow<NetworkCapabilities?> =
        ethernetFlow
            .map { event ->
                when (event) {
                    is TransportEvent.CapabilitiesChanged -> event.networkCapabilities
                    is TransportEvent.Lost -> null
                    else -> null
                }
            }
            .stateIn(applicationScope, SharingStarted.Eagerly, null)

    private suspend fun getSsidByDetectionMethod(
        detectionMethod: WifiDetectionMethod?,
        networkCapabilities: NetworkCapabilities?,
    ): String {
        val method = detectionMethod ?: DEFAULT
        return try {
                when (method) {
                        DEFAULT ->
                            networkCapabilities?.getWifiSsid()
                                ?: wifiManager?.getWifiSsid()
                                ?: ANDROID_UNKNOWN_SSID
                        LEGACY -> wifiManager?.getWifiSsid() ?: ANDROID_UNKNOWN_SSID
                        ROOT ->
                            withTimeoutOrNull(SHELL_COMMAND_TIMEOUT_MS) {
                                configurationListener.rootShell.getCurrentWifiName()
                            } ?: ANDROID_UNKNOWN_SSID
                        SHIZUKU ->
                            withTimeoutOrNull(SHELL_COMMAND_TIMEOUT_MS) {
                                ShizukuShell(applicationScope)
                                    .singleResponseCommand(WIFI_SSID_SHELL_COMMAND)
                            } ?: ANDROID_UNKNOWN_SSID
                    }
                    .trim()
                    .replace(Regex("[\n\r]"), "")
            } catch (e: Exception) {
                Timber.e(e, "Failed to get SSID with method: ${method.name}")
                ANDROID_UNKNOWN_SSID
            }
            .also { Timber.d("Current SSID via ${method.name}: $it") }
    }

    // default network events don't contain detailed capability information of underlying networks,
    // so we need to track separately
    private data class NetworkData(
        val defaultNetworkEvent: TransportEvent,
        val wifiCapabilities: NetworkCapabilities?,
        val cellularCaps: NetworkCapabilities?,
        val ethernetCaps: NetworkCapabilities?,
    )

    // combine our network flows to keep sync
    private val networkFlows: Flow<NetworkData> =
        combine(defaultNetworkFlow, wifiStateFlow, cellularStateFlow, ethernetStateFlow) {
            defaultEvent,
            wifiCaps,
            cellularCaps,
            ethernetCaps ->
            NetworkData(defaultEvent, wifiCaps, cellularCaps, ethernetCaps)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val connectivityStateFlow: SharedFlow<ConnectivityState> =
        combine(networkFlows, airplaneModeFlow, configurationListener.detectionMethod) {
                networkData,
                isAirplaneOn,
                detectionMethod ->
                val defaultEvent = networkData.defaultNetworkEvent
                val wifiCaps = networkData.wifiCapabilities
                val cellularCaps = networkData.cellularCaps
                val ethernetCaps = networkData.ethernetCaps

                // get the latest permissions info
                val permissions =
                    when (defaultEvent) {
                        is TransportEvent.Permissions -> defaultEvent.permissions
                        else ->
                            Permissions(
                                locationManager?.isLocationServicesEnabled() ?: false,
                                appContext.hasRequiredLocationPermissions(),
                            )
                    }

                val androidActiveNetwork = connectivityManager?.activeNetwork
                val defaultCaps =
                    when (defaultEvent) {
                        is TransportEvent.CapabilitiesChanged -> defaultEvent.networkCapabilities
                        else ->
                            androidActiveNetwork?.let {
                                connectivityManager?.getNetworkCapabilities(it)
                            }
                    }
                        ?: return@combine ConnectivityState(
                            ActiveNetwork.Disconnected,
                            permissions.locationServicesEnabled,
                            permissions.locationPermissionGranted,
                            isVpnActive = false,
                        )

                val vpnActive = defaultCaps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)

                // determine underlying network capabilities in order of Android's priority
                // (Ethernet > Wi-Fi > Cellular)
                val underlyingCaps = ethernetCaps ?: wifiCaps ?: cellularCaps

                // default caps will have detailed network info if VPN is not active
                val capsForValidation =
                    if (vpnActive) underlyingCaps ?: defaultCaps else defaultCaps

                // ensure validated internet connectivity
                // there is a known issue where Android will still report cellular connectivity if the
                // interface is not disabled and there is no connectivity (denoted by the '!')
                val isValidated =
                    capsForValidation.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                val hasInternet =
                    capsForValidation.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

                if (!isValidated || !hasInternet || (vpnActive && underlyingCaps == null)) {
                    return@combine ConnectivityState(
                        ActiveNetwork.Disconnected,
                        permissions.locationServicesEnabled,
                        permissions.locationPermissionGranted,
                        isVpnActive = vpnActive,
                    )
                }

                val activeNetwork: ActiveNetwork =
                    // if the VPN is active, we need to rely on capabilities from network flows as
                    // we won't have delayed underlying network info from default
                    if (vpnActive) {
                        when {
                            ethernetCaps != null -> ActiveNetwork.Ethernet
                            wifiCaps != null -> {
                                val ssid = getSsidByDetectionMethod(detectionMethod, wifiCaps)
                                ActiveNetwork.Wifi(ssid, wifiManager?.getCurrentSecurityType())
                            }
                            isAirplaneOn -> ActiveNetwork.Disconnected
                            cellularCaps != null -> ActiveNetwork.Cellular
                            else -> ActiveNetwork.Disconnected
                        }
                    } else {
                        // we can rely on default caps when VPN is not active
                        when {
                            defaultCaps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->
                                ActiveNetwork.Ethernet
                            defaultCaps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                                val ssid = getSsidByDetectionMethod(detectionMethod, defaultCaps)
                                ActiveNetwork.Wifi(ssid, wifiManager?.getCurrentSecurityType())
                            }
                            defaultCaps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) &&
                                !isAirplaneOn -> ActiveNetwork.Cellular
                            else -> ActiveNetwork.Disconnected
                        }
                    }

                ConnectivityState(
                    activeNetwork,
                    permissions.locationServicesEnabled,
                    permissions.locationPermissionGranted,
                    isVpnActive = vpnActive,
                )
            }
            .distinctUntilChanged()
            .flatMapLatest { state ->
                // prevent disconnected emits when VPN is activated
                if (state.activeNetwork is ActiveNetwork.Disconnected && state.isVpnActive) {
                    flow {
                        delay(1500)
                        emit(state)
                    }
                } else {
                    flowOf(state)
                }
            }
            .shareIn(applicationScope, SharingStarted.Eagerly, replay = 1)

    // utility to send local broadcast to trigger a recheck of location permissions onResume,
    // especially for getting SSID
    // that we did not have permission to read before, will trigger a recreation of the Wi-Fi flows
    // if permission was changed
    override fun checkPermissionsAndUpdateState() {
        val action = actionPermissionCheck
        val intent = Intent(action)
        Timber.d("Sending broadcast: $action")
        appContext.sendBroadcast(intent)
    }

    init {
        val exportedFlags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Context.RECEIVER_EXPORTED
            } else {
                0
            }
        val localFlags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Context.RECEIVER_NOT_EXPORTED
            } else {
                0
            }

        permissionReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == actionPermissionCheck) {
                        val isGranted = appContext.hasRequiredLocationPermissions()
                        Timber.d("Received permission check broadcast, isGranted: $isGranted")
                        if (
                            connectivityStateFlow.replayCache
                                .firstOrNull()
                                ?.locationPermissionsGranted != isGranted
                        ) {
                            Timber.d(
                                "Location permissions have changed, canceling and restarting callback flow"
                            )
                            permissionsChangedFlow.update { !permissionsChangedFlow.value }
                        }
                    }
                }
            }
        appContext.registerReceiver(
            permissionReceiver,
            IntentFilter(actionPermissionCheck),
            localFlags,
        )

        locationServicesReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == LOCATION_SERVICES_FILTER) {
                        Timber.d("Received location services broadcast")
                        val isLocationServicesEnabled = locationManager?.isLocationServicesEnabled()
                        if (
                            connectivityStateFlow.replayCache
                                .firstOrNull()
                                ?.locationServicesEnabled != isLocationServicesEnabled
                        ) {
                            Timber.d(
                                "Location services have changed, canceling and restarting callback flow"
                            )
                            permissionsChangedFlow.update { !permissionsChangedFlow.value }
                        }
                    }
                }
            }
        appContext.registerReceiver(
            locationServicesReceiver,
            IntentFilter(LOCATION_SERVICES_FILTER),
            exportedFlags,
        )

        airplaneReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == Intent.ACTION_AIRPLANE_MODE_CHANGED) {
                        Timber.d("Received airplane mode changed broadcast")
                        airplaneModeState.value = appContext.isAirplaneModeOn()
                    }
                }
            }
        appContext.registerReceiver(
            airplaneReceiver,
            IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED),
            exportedFlags,
        )
        airplaneModeState.update { appContext.isAirplaneModeOn() }
    }

    override fun destroy() {
        runCatching {
                permissionReceiver?.let { appContext.unregisterReceiver(it) }
                locationServicesReceiver?.let { appContext.unregisterReceiver(it) }
                airplaneReceiver?.let { appContext.unregisterReceiver(it) }

                defaultNetworkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
                wifiCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
                cellularCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
                ethernetCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
            }
            .onFailure { Timber.e(it, "Error during cleanup") }
        Timber.d("NetworkMonitor cleaned up")
    }
}
