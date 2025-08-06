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

    // Track active Wi-Fi networks, their capabilities, and last active network ID
    private val activeWifiNetworks = ActiveWifiStateManager()

    private val permissionsChangedFlow = MutableStateFlow(false)

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

        // The primary purpose of this receiver is to handle the case that the user enables location
        // permissions and then returns to the app
        // When this happens, we should check if permissions changed. If so, we need to requery
        // Wi-Fi name for the currently connected network
        val permissionReceiver =
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

        val locationServicesReceiver =
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

        val receiverFlags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Context.RECEIVER_EXPORTED // System broadcast
            } else {
                0
            }

        appContext.registerReceiver(
            permissionReceiver,
            IntentFilter(actionPermissionCheck),
            receiverFlags,
        )

        appContext.registerReceiver(
            locationServicesReceiver,
            IntentFilter(LOCATION_SERVICES_FILTER),
            receiverFlags,
        )

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
            activeWifiNetworks.put(network.toString(), Pair(network, null))
            trySend(TransportEvent.Available(network, detectionMethod))
        }

        fun handleOnWifiCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ) {
            Timber.d("Wi-Fi onCapabilitiesChanged: network=$network")
            activeWifiNetworks.put(network.toString(), Pair(network, networkCapabilities))
            trySend(
                TransportEvent.CapabilitiesChanged(network, networkCapabilities, detectionMethod)
            )
        }

        val callback =
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

        connectivityManager?.registerNetworkCallback(request, callback)

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
            runCatching {
                    appContext.unregisterReceiver(permissionReceiver)
                    appContext.unregisterReceiver(locationServicesReceiver)
                    connectivityManager?.unregisterNetworkCallback(callback)
                }
                .onFailure { Timber.e(it, "Error unregistering network callback") }
        }
    }

    private val cellularFlow: Flow<TransportEvent> = callbackFlow {
        val callback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Timber.d("Cellular onAvailable: network=$network")
                    trySend(TransportEvent.Available(network))
                }

                override fun onLost(network: Network) {
                    Timber.d("Cellular onLost: network=$network")
                    trySend(TransportEvent.Lost(network))
                }
            }

        val request =
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build()

        connectivityManager?.registerNetworkCallback(request, callback)
        trySend(TransportEvent.Unknown)

        awaitClose {
            runCatching { connectivityManager?.unregisterNetworkCallback(callback) }
                .onFailure { Timber.e(it, "Error unregistering cellular network callback") }
        }
    }

    private val ethernetFlow: Flow<TransportEvent> = callbackFlow {
        val callback =
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

        val request =
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                .build()

        connectivityManager?.registerNetworkCallback(request, callback)
        trySend(TransportEvent.Unknown)

        awaitClose {
            runCatching { connectivityManager?.unregisterNetworkCallback(callback) }
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
                            withTimeoutOrNull(2000) { // 2-second timeout
                                configurationListener.rootShell.getCurrentWifiName()
                            } ?: ANDROID_UNKNOWN_SSID
                        WifiDetectionMethod.SHIZUKU ->
                            withTimeoutOrNull(2000) { // 2-second timeout
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
}
