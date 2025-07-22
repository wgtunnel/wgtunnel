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
import com.zaneschepke.networkmonitor.util.WIFI_SSID_SHELL_COMMAND
import com.zaneschepke.networkmonitor.util.getCurrentSecurityType
import com.zaneschepke.networkmonitor.util.getCurrentWifiName
import com.zaneschepke.networkmonitor.util.getWifiSsid
import com.zaneschepke.networkmonitor.util.isLocationServicesEnabled
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import timber.log.Timber

class AndroidNetworkMonitor(
    private val appContext: Context,
    private val configurationListener: ConfigurationListener,
    private val applicationScope: CoroutineScope,
) : NetworkMonitor {

    interface ConfigurationListener {
        val detectionMethod: Flow<WifiDetectionMethod>
        val rootShell: RootShell
    }

    companion object {
        const val LOCATION_GRANTED = "LOCATION_PERMISSIONS_GRANTED"
        const val LOCATION_SERVICES_FILTER = "android.location.PROVIDERS_CHANGED"
        const val ANDROID_UNKNOWN_SSID = "<unknown ssid>"
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
    }

    private val packageName = appContext.packageName
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
    private val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
    private val locationManager =
        appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager?

    // Track active Wi-Fi networks, their capabilities, and last active network ID
    private val activeWifiNetworks = ActiveWifiStateManager()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val wifiFlow: Flow<TransportEvent> =
        configurationListener.detectionMethod.flatMapLatest { detectionMethod
            -> // cancels previous flow
            Timber.d("Updated detectionMethod=$detectionMethod, recreating wifiFlow")
            createWifiNetworkCallbackFlow(detectionMethod) // Create a new flow for each new method
        }

    private fun createWifiNetworkCallbackFlow(
        detectionMethod: WifiDetectionMethod
    ): Flow<TransportEvent> = callbackFlow {
        val locationPermissionReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    Timber.d(
                        "locationPermissionReceiver received intent with action: ${intent.action}"
                    )
                    if (intent.action == "$packageName.$LOCATION_GRANTED") {
                        Timber.d(
                            "Received update: Precise and all-the-time location permissions are enabled"
                        )
                        activeWifiNetworks.getLatestValue()?.let { details ->
                            trySend(
                                TransportEvent.LocationPermissionGranted(
                                    details.first,
                                    details.second,
                                    detectionMethod,
                                )
                            )
                        }
                    }
                }
            }

        val locationServicesReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == LOCATION_SERVICES_FILTER) {
                        val isGpsEnabled =
                            locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER)
                                ?: false
                        val isNetworkEnabled =
                            locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                                ?: false
                        val isLocationServicesEnabled = isGpsEnabled || isNetworkEnabled
                        Timber.d(
                            "Location Services state changed. Enabled: $isLocationServicesEnabled, GPS: $isGpsEnabled, Network: $isNetworkEnabled"
                        )
                        activeWifiNetworks.getLatestValue()?.let { details ->
                            trySend(
                                TransportEvent.LocationServicesChanged(
                                    isLocationServicesEnabled,
                                    details.first,
                                    details.second,
                                    detectionMethod,
                                )
                            )
                        }
                    }
                }
            }

        val permissionReceiverFlags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Context.RECEIVER_NOT_EXPORTED // Internal broadcast
            } else {
                0
            }

        val servicesReceiverFlags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Context.RECEIVER_EXPORTED // System broadcast
            } else {
                0
            }

        appContext.registerReceiver(
            locationPermissionReceiver,
            IntentFilter("$packageName.$LOCATION_GRANTED"),
            permissionReceiverFlags,
        )

        appContext.registerReceiver(
            locationServicesReceiver,
            IntentFilter(LOCATION_SERVICES_FILTER),
            servicesReceiverFlags,
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
            }

        val request =
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()

        connectivityManager?.registerNetworkCallback(request, callback)

        trySend(TransportEvent.Unknown)

        awaitClose {
            runCatching {
                    appContext.unregisterReceiver(locationPermissionReceiver)
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

    suspend fun getSsidByDetectionMethod(
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

    override val connectivityStateFlow =
        combine(
                wifiFlow.scan(
                    WifiState(
                        locationPermissionsGranted =
                            ContextCompat.checkSelfPermission(
                                appContext,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                            ) == PackageManager.PERMISSION_GRANTED,
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
                        is TransportEvent.LocationPermissionGranted ->
                            previous.copy(
                                locationPermissionsGranted = true,
                                ssid =
                                    getSsidByDetectionMethod(
                                        event.wifiDetectionMethod,
                                        event.networkCapabilities,
                                    ),
                                securityType = wifiManager?.getCurrentSecurityType(),
                            )
                        is TransportEvent.LocationServicesChanged ->
                            previous.copy(
                                locationServicesEnabled = event.enabled,
                                ssid =
                                    getSsidByDetectionMethod(
                                        event.wifiDetectionMethod,
                                        event.networkCapabilities,
                                    ),
                                securityType = wifiManager?.getCurrentSecurityType(),
                            )
                        is TransportEvent.Lost ->
                            previous.copy(connected = false, securityType = null, ssid = null)
                        TransportEvent.Unknown -> previous
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
            .shareIn(applicationScope, SharingStarted.WhileSubscribed(5000), replay = 1)
}
