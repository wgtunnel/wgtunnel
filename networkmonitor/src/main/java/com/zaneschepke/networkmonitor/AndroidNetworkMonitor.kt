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
import com.zaneschepke.networkmonitor.shizuku.ShizukuShell
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
                WifiDetectionMethod.entries.find { it.value == value } ?: DEFAULT
        }
    }

    private val packageName = appContext.packageName
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
    private val locationManager =
        appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val wifiMutex = Mutex()

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private var currentSsid: String? = null
    private var securityType: WifiSecurityType? = null

    private var wifiConnected = false

    // Track active Wi-Fi networks and last active network ID
    private val activeWifiNetworks = mutableSetOf<String>()

    data class WifiState(
        val connected: Boolean = false,
        val ssid: String? = null,
        val securityType: WifiSecurityType? = null,
    )

    data class TransportState(val connected: Boolean = false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val wifiFlow: Flow<WifiState> =
        configurationListener.detectionMethod.flatMapLatest { detectionMethod
            -> // cancels previous flow
            createWifiNetworkCallbackFlow(detectionMethod) // Create a new flow for each new method
        }

    private fun createWifiNetworkCallbackFlow(
        detectionMethod: WifiDetectionMethod
    ): Flow<WifiState> = callbackFlow {
        @Suppress("DEPRECATION")
        suspend fun getWifiSsid(): String {
            return withContext(ioDispatcher) {
                if (wifiManager == null) return@withContext ANDROID_UNKNOWN_SSID
                try {
                    wifiManager.connectionInfo?.ssid?.trim('"')?.takeIf { it.isNotEmpty() }
                        ?: ANDROID_UNKNOWN_SSID
                } catch (e: Exception) {
                    Timber.e(e)
                    ANDROID_UNKNOWN_SSID
                }
            }
        }

        suspend fun handleUnknownWifi() {
            wifiMutex.withLock {
                val newSsid = getWifiSsid()
                val securityType = wifiManager?.getCurrentSecurityType()
                // Only update if new SSID is valid; preserve existing valid SSID otherwise
                if (newSsid != WifiManager.UNKNOWN_SSID) {
                    currentSsid = newSsid
                    trySend(WifiState(wifiConnected, currentSsid, securityType))
                } else if (currentSsid == null || currentSsid == WifiManager.UNKNOWN_SSID) {
                    currentSsid = newSsid
                    trySend(WifiState(wifiConnected, currentSsid, securityType))
                }
            }
        }

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
                        applicationScope.launch { handleUnknownWifi() }
                    }
                }
            }

        val locationServicesReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == LOCATION_SERVICES_FILTER) {
                        val isGpsEnabled =
                            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                        val isNetworkEnabled =
                            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                        val isLocationServicesEnabled = isGpsEnabled || isNetworkEnabled
                        Timber.d(
                            "Location Services state changed. Enabled: $isLocationServicesEnabled, GPS: $isGpsEnabled, Network: $isNetworkEnabled"
                        )
                        if (isLocationServicesEnabled)
                            applicationScope.launch { handleUnknownWifi() }
                    }
                }
            }

        // Use RECEIVER_NOT_EXPORTED for Android 14+ compatibility
        val flags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Context.RECEIVER_EXPORTED
            } else {
                0
            }

        appContext.registerReceiver(
            locationPermissionReceiver,
            IntentFilter("$packageName.$LOCATION_GRANTED"),
            flags,
        )

        appContext.registerReceiver(
            locationServicesReceiver,
            IntentFilter(LOCATION_SERVICES_FILTER),
            flags,
        )

        suspend fun handleOnWifiLost(network: Network) {
            wifiMutex.withLock {
                Timber.d("Wi-Fi onLost: network=$network")
                activeWifiNetworks.remove(network.toString())
                if (activeWifiNetworks.isEmpty()) {
                    Timber.d(
                        "All Wi-Fi networks disconnected, clearing currentSsid and wifiConnected"
                    )
                    currentSsid = null
                    wifiConnected = false
                    trySend(WifiState(connected = false, ssid = null, securityType = null))
                } else {
                    Timber.d("Wi-Fi onLost, but still connected to other networks, ignoring")
                }
            }
        }

        suspend fun handleOnWifiAvailable(
            network: Network,
            networkCapabilities: NetworkCapabilities?,
        ) {
            wifiMutex.withLock {
                Timber.d("Wi-Fi onAvailable: network=$network")
                activeWifiNetworks.add(network.toString())
                currentSsid =
                    try {
                            when (detectionMethod) {
                                    WifiDetectionMethod.DEFAULT ->
                                        networkCapabilities?.getWifiSsid() ?: getWifiSsid()
                                    WifiDetectionMethod.LEGACY -> getWifiSsid()
                                    WifiDetectionMethod.ROOT ->
                                        configurationListener.rootShell.getCurrentWifiName()
                                    WifiDetectionMethod.SHIZUKU ->
                                        ShizukuShell(applicationScope)
                                            .singleResponseCommand(WIFI_SSID_SHELL_COMMAND)
                                }
                                .trim()
                                .replace(Regex("[\n\r]"), "")
                        } catch (e: Exception) {
                            Timber.e(e)
                            ANDROID_UNKNOWN_SSID
                        }
                        .also { Timber.d("Current SSID via ${detectionMethod.name}: $it") }
                securityType = wifiManager?.getCurrentSecurityType()
                wifiConnected = true
                trySend(
                    WifiState(connected = true, ssid = currentSsid, securityType = securityType)
                )
            }
        }

        val callback =
            when {
                detectionMethod == WifiDetectionMethod.LEGACY ||
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ->
                    object : ConnectivityManager.NetworkCallback() {
                        override fun onAvailable(network: Network) {
                            applicationScope.launch { handleOnWifiAvailable(network, null) }
                        }

                        override fun onLost(network: Network) {
                            applicationScope.launch { handleOnWifiLost(network) }
                        }
                    }
                else ->
                    object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {

                        override fun onAvailable(network: Network) {
                            if (detectionMethod != WifiDetectionMethod.DEFAULT)
                                applicationScope.launch { handleOnWifiAvailable(network, null) }
                        }

                        override fun onCapabilitiesChanged(
                            network: Network,
                            networkCapabilities: NetworkCapabilities,
                        ) {
                            if (detectionMethod == WifiDetectionMethod.DEFAULT)
                                applicationScope.launch {
                                    handleOnWifiAvailable(network, networkCapabilities)
                                }
                        }

                        override fun onLost(network: Network) {
                            applicationScope.launch { handleOnWifiLost(network) }
                        }
                    }
            }

        val request =
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()

        connectivityManager.registerNetworkCallback(request, callback)
        trySend(WifiState())

        awaitClose {
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (e: IllegalArgumentException) {
                Timber.e(
                    e,
                    "Flow failed to unregister NetworkCallback, was already unregistered or not registered correctly.",
                )
            }
            appContext.unregisterReceiver(locationPermissionReceiver)
            appContext.unregisterReceiver(locationServicesReceiver)
        }
    }

    private val cellularFlow: Flow<TransportState> = callbackFlow {
        val callback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Timber.d("Cellular onAvailable: network=$network")
                    trySend(TransportState(connected = true))
                }

                override fun onLost(network: Network) {
                    Timber.d("Cellular onLost: network=$network")
                    trySend(TransportState(connected = false))
                }
            }

        val request =
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build()

        connectivityManager.registerNetworkCallback(request, callback)
        trySend(TransportState())

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }

    private val ethernetFlow: Flow<TransportState> = callbackFlow {
        val callback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Timber.d("Ethernet onAvailable: network=$network")
                    trySend(TransportState(connected = true))
                }

                override fun onLost(network: Network) {
                    Timber.d("Ethernet onLost: network=$network")
                    trySend(TransportState(connected = false))
                }
            }

        val request =
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                .build()

        connectivityManager.registerNetworkCallback(request, callback)
        trySend(TransportState())

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }

    override val networkStatusFlow =
        combine(wifiFlow, cellularFlow, ethernetFlow) { wifi, cellular, ethernet ->
                val hasAnyConnection = wifi.connected || cellular.connected || ethernet.connected
                if (hasAnyConnection) {
                        NetworkStatus.Connected(
                            wifiSsid = wifi.ssid,
                            securityType = wifi.securityType,
                            wifiConnected = wifi.connected,
                            cellularConnected = cellular.connected,
                            ethernetConnected = ethernet.connected,
                        )
                    } else {
                        NetworkStatus.Disconnected
                    }
                    .also { Timber.d("NetworkStatus: $it") }
            }
            .distinctUntilChanged()
            .shareIn(applicationScope, SharingStarted.WhileSubscribed(5000), replay = 1)

    override fun sendLocationPermissionsGrantedBroadcast() {
        val action = "$packageName.$LOCATION_GRANTED"
        val intent = Intent(action)
        Timber.d("Sending broadcast: $action")
        appContext.sendBroadcast(intent)
    }
}
