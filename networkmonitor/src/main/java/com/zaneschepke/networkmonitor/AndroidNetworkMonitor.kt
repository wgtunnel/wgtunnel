package com.zaneschepke.networkmonitor

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.wireguard.android.util.RootShell
import com.zaneschepke.networkmonitor.shizuku.ShizukuShell
import com.zaneschepke.networkmonitor.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

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
        const val WIFI_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_STATE_CHANGED"
        const val CONNECTIVITY_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE"
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

    private val activeWifiNetworks =
        ConcurrentHashMap<String, Pair<Network?, NetworkCapabilities?>>()

    private val activeCellularNetworks =
        ConcurrentHashMap<String, Pair<Network?, NetworkCapabilities?>>()

    private val permissionsChangedFlow = MutableStateFlow(false)

    private var permissionReceiver: BroadcastReceiver? = null
    private var locationServicesReceiver: BroadcastReceiver? = null
    private var wifiStateReceiver: BroadcastReceiver? = null
    private var cellularDataReceiver: BroadcastReceiver? = null
    private var wifiCallback: ConnectivityManager.NetworkCallback? = null
    private var cellularCallback: ConnectivityManager.NetworkCallback? = null
    private var ethernetCallback: ConnectivityManager.NetworkCallback? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    private val wifiFlow: Flow<TransportEvent> =
        combine(configurationListener.detectionMethod, permissionsChangedFlow) {
                detectionMethod,
                changed ->
                Pair(detectionMethod, changed)
            }
            .flatMapLatest { (detectionMethod, _) -> // cancels previous flow
                Timber.d("Permission or detection method changed, recreating wifiFlow")
                createWifiNetworkCallbackFlow(detectionMethod)
            }

    private fun isAndroidTv(): Boolean =
        appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

    private fun hasRequiredLocationPermissions(): Boolean {
        val fineLocationGranted =
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        val backgroundLocationGranted =
            if (
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) &&
                    // exclude Android TV on Q as background location is not required on this
                    // version
                    !(Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && isAndroidTv())
            ) {
                ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true // No need for ACCESS_BACKGROUND_LOCATION on Android P or Android TV on Q
            }
        return fineLocationGranted && backgroundLocationGranted
    }

    private fun createWifiNetworkCallbackFlow(
        detectionMethod: WifiDetectionMethod
    ): Flow<TransportEvent> = callbackFlow {
        fun handleOnWifiLost(network: Network) {
            Timber.d("Wi-Fi onLost: network=$network")
            activeWifiNetworks.remove(network.toString())
            if (activeWifiNetworks.isEmpty()) {
                Timber.d("All Wi-Fi networks disconnected, clearing currentSsid and wifiConnected")
                trySend(TransportEvent.Lost(network))
            } else {
                Timber.d("Wi-Fi onLost, but still connected to other networks, ignoring")
                // This can happen when switching between APs of the same SSID
            }
        }

        fun handleOnWifiAvailable(network: Network) {
            Timber.d("Wi-Fi onAvailable: network=$network")
            activeWifiNetworks[network.toString()] = Pair(network, null)
            trySend(TransportEvent.Available(network, detectionMethod))
        }

        fun handleOnWifiCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ) {
            Timber.d("Wi-Fi onCapabilitiesChanged: network=$network")
            activeWifiNetworks[network.toString()] = Pair(network, networkCapabilities)
            trySend(
                TransportEvent.CapabilitiesChanged(network, networkCapabilities, detectionMethod)
            )
        }

        wifiCallback =
            when {
                detectionMethod == WifiDetectionMethod.LEGACY ||
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ->
                    object : ConnectivityManager.NetworkCallback() {
                            override fun onAvailable(network: Network) {
                                handleOnWifiAvailable(network)
                            }

                            override fun onLost(network: Network) {
                                handleOnWifiLost(network)
                            }
                        }
                        .also { Timber.d("Creating Wi-Fi callback without location info flags") }
                else ->
                    object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {

                            override fun onAvailable(network: Network) {
                                if (detectionMethod != WifiDetectionMethod.DEFAULT)
                                    handleOnWifiAvailable(network)
                            }

                            override fun onCapabilitiesChanged(
                                network: Network,
                                networkCapabilities: NetworkCapabilities,
                            ) {
                                if (detectionMethod == WifiDetectionMethod.DEFAULT)
                                    handleOnWifiCapabilitiesChanged(network, networkCapabilities)
                            }

                            override fun onLost(network: Network) {
                                handleOnWifiLost(network)
                            }
                        }
                        .also { Timber.d("Creating Wi-Fi callback with location info flags") }
            }

        val request =
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()

        connectivityManager?.registerNetworkCallback(request, wifiCallback!!)

        trySend(
            TransportEvent.Permissions(
                permissions =
                    Permissions(
                        locationManager?.isLocationServicesEnabled() ?: false,
                        hasRequiredLocationPermissions(),
                    )
            )
        )

        awaitClose {
            runCatching { connectivityManager?.unregisterNetworkCallback(wifiCallback!!) }
                .onFailure { Timber.e(it, "Error unregistering network callback") }
        }
    }

    private val cellularFlow: Flow<TransportEvent> = callbackFlow {
        val cellularLocalCallback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Timber.d("Cellular onAvailable: network=$network")
                    activeCellularNetworks[network.toString()] = Pair(network, null)
                    trySend(TransportEvent.Available(network))
                }

                override fun onLost(network: Network) {
                    Timber.d("Cellular onLost: network=$network")
                    activeCellularNetworks.remove(network.toString())
                    if (activeCellularNetworks.isEmpty()) {
                        Timber.d("All cellular networks disconnected")
                        trySend(TransportEvent.Lost(network))
                    } else {
                        Timber.d("Cellular onLost, but still connected to other, ignoring")
                    }
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities,
                ) {
                    Timber.d("Cellular onCapabilitiesChanged: network=$network")
                    activeCellularNetworks[network.toString()] = Pair(network, networkCapabilities)
                }
            }
        cellularCallback = cellularLocalCallback

        val request =
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
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
        val ethernetLocalCallback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Timber.d("Ethernet onAvailable: network=$network")
                    trySend(TransportEvent.Available(network))
                }

                override fun onLost(network: Network) {
                    Timber.d("Ethernet onLost: network=$network")
                    trySend(TransportEvent.Lost(network))
                }
            }
        ethernetCallback = ethernetLocalCallback

        val request =
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                .build()

        connectivityManager?.registerNetworkCallback(request, ethernetCallback!!)
        trySend(TransportEvent.Unknown)

        awaitClose {
            runCatching { connectivityManager?.unregisterNetworkCallback(ethernetCallback!!) }
                .onFailure { Timber.e(it, "Error unregistering ethernet network callback") }
        }
    }

    private suspend fun getSsidByDetectionMethod(
        detectionMethod: WifiDetectionMethod?,
        networkCapabilities: NetworkCapabilities?,
    ): String {
        val method = detectionMethod ?: WifiDetectionMethod.DEFAULT
        return try {
                when (method) {
                        WifiDetectionMethod.DEFAULT ->
                            networkCapabilities?.getWifiSsid()
                                ?: wifiManager?.getWifiSsid()
                                ?: ANDROID_UNKNOWN_SSID
                        WifiDetectionMethod.LEGACY ->
                            wifiManager?.getWifiSsid() ?: ANDROID_UNKNOWN_SSID
                        WifiDetectionMethod.ROOT ->
                            withTimeoutOrNull(SHELL_COMMAND_TIMEOUT_MS) {
                                configurationListener.rootShell.getCurrentWifiName()
                            } ?: ANDROID_UNKNOWN_SSID
                        WifiDetectionMethod.SHIZUKU ->
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

    override val connectivityStateFlow: SharedFlow<ConnectivityState> =
        combine(
                wifiFlow.scan(
                    WifiState(
                        locationPermissionsGranted = hasRequiredLocationPermissions(),
                        locationServicesEnabled =
                            locationManager?.isLocationServicesEnabled() ?: false,
                    )
                ) { previous, event ->
                    when (event) {
                        is TransportEvent.Available ->
                            previous.copy(
                                connected = true,
                                ssid =
                                    getSsidByDetectionMethod(
                                        event.wifiDetectionMethod ?: WifiDetectionMethod.DEFAULT,
                                        null,
                                    ),
                                securityType = wifiManager?.getCurrentSecurityType(),
                            )
                        is TransportEvent.CapabilitiesChanged ->
                            previous.copy(
                                connected = true,
                                ssid =
                                    getSsidByDetectionMethod(
                                        event.wifiDetectionMethod ?: WifiDetectionMethod.DEFAULT,
                                        null,
                                    ),
                                securityType = wifiManager?.getCurrentSecurityType(),
                            )
                        is TransportEvent.Permissions -> {
                            previous.copy(
                                locationPermissionsGranted =
                                    event.permissions.locationPermissionGranted,
                                locationServicesEnabled = event.permissions.locationServicesEnabled,
                            )
                        }
                        is TransportEvent.Lost ->
                            previous.copy(connected = false, securityType = null, ssid = null)
                        is TransportEvent.Unknown -> previous
                    }
                },
                cellularFlow,
                ethernetFlow,
            ) { wifi, cellular, ethernet ->
                val cellularConnected = cellular is TransportEvent.Available
                val ethernetConnected = ethernet is TransportEvent.Available
                ConnectivityState(
                        wifi,
                        cellularConnected = cellularConnected,
                        ethernetConnected = ethernetConnected,
                    )
                    .also { Timber.d("Connectivity Status: $it") }
            }
            .distinctUntilChanged()
            .shareIn(applicationScope, SharingStarted.Eagerly, replay = 1)

    override fun checkPermissionsAndUpdateState() {
        val action = actionPermissionCheck
        val intent = Intent(action)
        Timber.d("Sending broadcast: $action")
        appContext.sendBroadcast(intent)
    }

    init {
        val receiverFlags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Context.RECEIVER_EXPORTED // System broadcast
            } else {
                0
            }

        permissionReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == actionPermissionCheck) {
                        val isGranted = hasRequiredLocationPermissions()
                        Timber.d("Received permission check broadcast, isGranted: $isGranted")
                        // get Wi-Fi info on permission change and update permission state
                        if (
                            connectivityStateFlow.replayCache
                                .firstOrNull()
                                ?.wifiState
                                ?.locationPermissionsGranted != isGranted
                        ) {
                            Timber.d(
                                "Location permissions have changed, canceling and restarting callback flow"
                            )
                            activeWifiNetworks.clear()
                            permissionsChangedFlow.update { !permissionsChangedFlow.value }
                        }
                    }
                }
            }

        permissionReceiver?.let {
            appContext.registerReceiver(it, IntentFilter(actionPermissionCheck), receiverFlags)
        }

        locationServicesReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == LOCATION_SERVICES_FILTER) {
                        Timber.d("Received location services broadcast")
                        val isLocationServicesEnabled = locationManager?.isLocationServicesEnabled()
                        if (
                            connectivityStateFlow.replayCache
                                .firstOrNull()
                                ?.wifiState
                                ?.locationServicesEnabled != isLocationServicesEnabled
                        ) {
                            Timber.d(
                                "Location services have changed, canceling and restarting callback flow"
                            )
                            // trigger cancel and recreate of callbackFlow
                            activeWifiNetworks.clear()
                            permissionsChangedFlow.update { !permissionsChangedFlow.value }
                        }
                    }
                }
            }
        locationServicesReceiver?.let {
            appContext.registerReceiver(
                locationServicesReceiver,
                IntentFilter(LOCATION_SERVICES_FILTER),
                receiverFlags,
            )
        }

        wifiStateReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == WIFI_STATE_CHANGED_ACTION) {
                        val wifiState =
                            intent.getIntExtra(
                                WifiManager.EXTRA_WIFI_STATE,
                                WifiManager.WIFI_STATE_UNKNOWN,
                            )
                        Timber.d("WiFi state changed to: $wifiState")
                        when (wifiState) {
                            WifiManager.WIFI_STATE_DISABLED -> {
                                Timber.d("WiFi disabled, forcing requery")
                                activeWifiNetworks.clear()
                                permissionsChangedFlow.update { !permissionsChangedFlow.value }
                            }
                            WifiManager.WIFI_STATE_ENABLED -> {
                                Timber.d("WiFi enabled, forcing requery")
                                permissionsChangedFlow.update { !permissionsChangedFlow.value }
                            }
                        }
                    }
                }
            }
        wifiStateReceiver?.let {
            appContext.registerReceiver(it, IntentFilter(WIFI_STATE_CHANGED_ACTION), receiverFlags)
        }

        cellularDataReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == CONNECTIVITY_CHANGE) {
                        Timber.d("Connectivity change detected")
                        val activeNet = connectivityManager?.activeNetwork
                        val caps = activeNet?.let { connectivityManager.getNetworkCapabilities(it) }
                        if (
                            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true &&
                                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        ) {
                            if (activeCellularNetworks.isEmpty()) {
                                activeCellularNetworks[activeNet.toString()] = Pair(activeNet, caps)
                                // callback should handle change, no emit needed

                            }
                        } else if (activeCellularNetworks.isNotEmpty()) {
                            val lostNetworks = activeCellularNetworks.values.mapNotNull { it.first }
                            activeCellularNetworks.clear()
                            // callback should handle change, no emit needed
                        }
                    }
                }
            }
        cellularDataReceiver?.let {
            appContext.registerReceiver(it, IntentFilter(CONNECTIVITY_CHANGE), receiverFlags)
        }
    }

    override fun destroy() {
        runCatching {
                permissionReceiver?.let { appContext.unregisterReceiver(it) }
                locationServicesReceiver?.let { appContext.unregisterReceiver(it) }
                wifiStateReceiver?.let { appContext.unregisterReceiver(it) }
                cellularDataReceiver?.let { appContext.unregisterReceiver(it) }
                wifiCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
                cellularCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
                ethernetCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
            }
            .onFailure { Timber.e(it, "Error during cleanup") }
        Timber.d("NetworkMonitor cleaned up")
    }
}
