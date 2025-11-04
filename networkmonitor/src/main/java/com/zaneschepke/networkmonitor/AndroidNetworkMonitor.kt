package com.zaneschepke.networkmonitor

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback.FLAG_INCLUDE_LOCATION_INFO
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.wireguard.android.util.RootShell
import com.zaneschepke.networkmonitor.AndroidNetworkMonitor.WifiDetectionMethod.*
import com.zaneschepke.networkmonitor.shizuku.ShizukuShell
import com.zaneschepke.networkmonitor.util.*
import java.util.concurrent.ConcurrentHashMap
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

    private val permissionsChangedFlow = MutableStateFlow(false)

    private var permissionReceiver: BroadcastReceiver? = null
    private var locationServicesReceiver: BroadcastReceiver? = null
    private var defaultNetworkCallback: ConnectivityManager.NetworkCallback? = null
    private var wifiInterfaceCallback: ConnectivityManager.NetworkCallback? = null
    private var cellularInterfaceCallback: ConnectivityManager.NetworkCallback? = null

    private val isAirplaneModeOn: Boolean
        get() =
            android.provider.Settings.Global.getInt(
                appContext.contentResolver,
                android.provider.Settings.Global.AIRPLANE_MODE_ON,
                0,
            ) != 0

    @OptIn(ExperimentalCoroutinesApi::class)
    private val defaultNetworkFlow: Flow<TransportEvent> =
        combine(configurationListener.detectionMethod, permissionsChangedFlow) {
                detectionMethod,
                changed ->
                Pair(detectionMethod, changed)
            }
            .flatMapLatest { (detectionMethod, _) ->
                createDefaultNetworkCallbackFlow(detectionMethod)
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

    private fun createDefaultNetworkCallbackFlow(
        detectionMethod: WifiDetectionMethod
    ): Flow<TransportEvent> = callbackFlow {
        val onAvailable: (Network) -> Unit = { network ->
            Timber.d("Network onAvailable: network=$network")
        }

        val onLost: (Network) -> Unit = { network ->
            Timber.d("Network onLost: network=$network")
            trySend(TransportEvent.Lost(network))
        }

        val onCapabilitiesChanged: (Network, NetworkCapabilities) -> Unit =
            { network, networkCapabilities ->
                val isValidated =
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                val hasInternet =
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

                Timber.d("onCapabilitiesChanged: network=$network, validated: $isValidated")

                if (isValidated && hasInternet) {
                    val event =
                        when {
                            networkCapabilities.hasTransport(
                                NetworkCapabilities.TRANSPORT_WIFI
                            ) -> {
                                activeWifiNetworks[network.toString()] =
                                    Pair(network, networkCapabilities)
                                TransportEvent.CapabilitiesChanged(
                                    network,
                                    networkCapabilities,
                                    detectionMethod,
                                )
                            }
                            networkCapabilities.hasTransport(
                                NetworkCapabilities.TRANSPORT_CELLULAR
                            ) -> {
                                activeWifiNetworks.clear()
                                TransportEvent.CapabilitiesChanged(network, networkCapabilities)
                            }
                            networkCapabilities.hasTransport(
                                NetworkCapabilities.TRANSPORT_ETHERNET
                            ) -> {
                                activeWifiNetworks.clear()
                                TransportEvent.CapabilitiesChanged(network, networkCapabilities)
                            }
                            else -> TransportEvent.Unknown
                        }
                    trySend(event)
                } else {
                    activeWifiNetworks.remove(network.toString())
                    trySend(TransportEvent.Lost(network))
                }
            }

        val callback: ConnectivityManager.NetworkCallback =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && detectionMethod == DEFAULT) {
                object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
                    override fun onAvailable(network: Network) = onAvailable(network)

                    override fun onLost(network: Network) = onLost(network)

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities,
                    ) = onCapabilitiesChanged(network, networkCapabilities)
                }
            } else {
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) = onAvailable(network)

                    override fun onLost(network: Network) = onLost(network)

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities,
                    ) = onCapabilitiesChanged(network, networkCapabilities)
                }
            }
        defaultNetworkCallback = callback

        connectivityManager?.registerDefaultNetworkCallback(defaultNetworkCallback!!)

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
            runCatching { connectivityManager?.unregisterNetworkCallback(defaultNetworkCallback!!) }
                .onFailure { Timber.e(it, "Error unregistering default network callback") }
        }
    }

    private val wifiInterfaceFlow: Flow<Boolean> = callbackFlow {
        val localCallback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Timber.d("Wi-Fi onAvailable: network=$network")
                    trySend(true)
                }

                override fun onLost(network: Network) {
                    Timber.d("Wi-Fi onLost: network=$network")
                    trySend(false)
                }
            }
        wifiInterfaceCallback = localCallback

        val request =
            NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()

        connectivityManager?.registerNetworkCallback(request, wifiInterfaceCallback!!)

        @Suppress("DEPRECATION") val isWifiInitiallyOn = wifiManager?.isWifiEnabled == true
        trySend(isWifiInitiallyOn)

        awaitClose {
            runCatching { connectivityManager?.unregisterNetworkCallback(wifiInterfaceCallback!!) }
                .onFailure { Timber.e(it, "Error unregistering Wi-Fi interface callback") }
        }
    }

    private val cellularInterfaceFlow: Flow<Boolean> = callbackFlow {
        val localCallback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Timber.d("Cellular onAvailable: network=$network")
                    trySend(true)
                }

                override fun onLost(network: Network) {
                    Timber.d("Cellular onLost: network=$network")
                    trySend(false)
                }
            }
        cellularInterfaceCallback = localCallback

        val request =
            NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build()

        connectivityManager?.registerNetworkCallback(request, cellularInterfaceCallback!!)

        // initial state
        val initialCellularNetwork = connectivityManager?.activeNetwork
        val initialCapabilities =
            connectivityManager?.getNetworkCapabilities(initialCellularNetwork)
        val isCellularInitiallyOn =
            initialCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        trySend(isCellularInitiallyOn)

        awaitClose {
            runCatching {
                    connectivityManager?.unregisterNetworkCallback(cellularInterfaceCallback!!)
                }
                .onFailure { Timber.e(it, "Error unregistering cellular interface callback") }
        }
    }

    private val airplaneModeFlow: Flow<Boolean> = callbackFlow {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == Intent.ACTION_AIRPLANE_MODE_CHANGED) {
                        Timber.d("Received airplane mode changed broadcast")
                        trySend(isAirplaneModeOn)
                    }
                }
            }

        val filter = IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        appContext.registerReceiver(receiver, filter)

        // initial state
        trySend(isAirplaneModeOn)

        awaitClose {
            runCatching { appContext.unregisterReceiver(receiver) }
                .onFailure { Timber.e(it, "Error unregistering airplane mode receiver") }
        }
    }

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

    override val connectivityStateFlow: SharedFlow<ConnectivityState> =
        combine(
                defaultNetworkFlow.scan(
                    ConnectivityState(
                        activeNetwork = ActiveNetwork.Disconnected,
                        locationPermissionsGranted = hasRequiredLocationPermissions(),
                        locationServicesEnabled =
                            locationManager?.isLocationServicesEnabled() ?: false,
                    )
                ) { previous, event ->
                    when (event) {
                        is TransportEvent.CapabilitiesChanged -> {
                            when {
                                event.networkCapabilities.hasTransport(
                                    NetworkCapabilities.TRANSPORT_WIFI
                                ) -> {
                                    val ssid =
                                        getSsidByDetectionMethod(
                                            event.wifiDetectionMethod
                                                ?: WifiDetectionMethod.DEFAULT,
                                            event.networkCapabilities,
                                        )

                                    previous.copy(
                                        activeNetwork =
                                            ActiveNetwork.Wifi(
                                                ssid = ssid,
                                                securityType = wifiManager?.getCurrentSecurityType(),
                                            )
                                    )
                                }
                                event.networkCapabilities.hasTransport(
                                    NetworkCapabilities.TRANSPORT_CELLULAR
                                ) -> {
                                    activeWifiNetworks.clear()
                                    previous.copy(activeNetwork = ActiveNetwork.Cellular)
                                }
                                event.networkCapabilities.hasTransport(
                                    NetworkCapabilities.TRANSPORT_ETHERNET
                                ) -> {
                                    activeWifiNetworks.clear()
                                    previous.copy(activeNetwork = ActiveNetwork.Ethernet)
                                }
                                else -> previous
                            }
                        }
                        is TransportEvent.Lost ->
                            previous.copy(activeNetwork = ActiveNetwork.Disconnected)
                        is TransportEvent.Permissions -> {
                            previous.copy(
                                locationPermissionsGranted =
                                    event.permissions.locationPermissionGranted,
                                locationServicesEnabled = event.permissions.locationServicesEnabled,
                            )
                        }
                        is TransportEvent.Available -> previous
                        is TransportEvent.Unknown -> previous
                    }
                },
                wifiInterfaceFlow,
                airplaneModeFlow,
                cellularInterfaceFlow,
            ) { defaultState, isWifiInterfaceOn, isAirplaneModeOn, isCellularInterfaceOn ->
                val activeNetwork =
                    when {
                        // Wi-Fi interface disabled, force disconnected
                        !isWifiInterfaceOn && defaultState.activeNetwork is ActiveNetwork.Wifi ->
                            ActiveNetwork.Disconnected
                        // Cellular active when airplane mode on
                        isAirplaneModeOn && defaultState.activeNetwork is ActiveNetwork.Cellular ->
                            ActiveNetwork.Disconnected
                        // Cellular active when cellular interface disabled
                        !isCellularInterfaceOn &&
                            defaultState.activeNetwork is ActiveNetwork.Cellular ->
                            ActiveNetwork.Disconnected
                        else -> defaultState.activeNetwork
                    }

                ConnectivityState(
                        activeNetwork = activeNetwork,
                        locationPermissionsGranted = defaultState.locationPermissionsGranted,
                        locationServicesEnabled = defaultState.locationServicesEnabled,
                    )
                    .also { Timber.i("Connectivity Status: $it") }
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
                Context.RECEIVER_EXPORTED
            } else {
                0
            }

        permissionReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == actionPermissionCheck) {
                        val isGranted = hasRequiredLocationPermissions()
                        Timber.d("Received permission check broadcast, isGranted: $isGranted")
                        if (
                            connectivityStateFlow.replayCache
                                .firstOrNull()
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
                                ?.locationServicesEnabled != isLocationServicesEnabled
                        ) {
                            Timber.d(
                                "Location services have changed, canceling and restarting callback flow"
                            )
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
    }

    override fun destroy() {
        runCatching {
                permissionReceiver?.let { appContext.unregisterReceiver(it) }
                locationServicesReceiver?.let { appContext.unregisterReceiver(it) }

                defaultNetworkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
                wifiInterfaceCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
                cellularInterfaceCallback?.let {
                    connectivityManager?.unregisterNetworkCallback(it)
                }
            }
            .onFailure { Timber.e(it, "Error during cleanup") }
        Timber.d("NetworkMonitor cleaned up")
    }
}
