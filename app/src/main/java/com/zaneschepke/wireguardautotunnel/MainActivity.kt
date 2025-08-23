package com.zaneschepke.wireguardautotunnel

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.zaneschepke.networkmonitor.NetworkMonitor
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.data.AppDatabase
import com.zaneschepke.wireguardautotunnel.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.di.MainDispatcher
import com.zaneschepke.wireguardautotunnel.domain.repository.AppStateRepository
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.banner.AppAlertBanner
import com.zaneschepke.wireguardautotunnel.ui.common.dialog.VpnDeniedDialog
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.CustomSnackBar
import com.zaneschepke.wireguardautotunnel.ui.navigation.LocalIsAndroidTV
import com.zaneschepke.wireguardautotunnel.ui.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.navigation.components.BottomNavbar
import com.zaneschepke.wireguardautotunnel.ui.navigation.components.DynamicTopAppBar
import com.zaneschepke.wireguardautotunnel.ui.navigation.components.currentNavBackStackEntryAsNavBarState
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.AutoTunnelScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.advanced.AutoTunnelAdvancedScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.detection.WifiDetectionMethodScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.disclosure.LocationDisclosureScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.main.MainScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.main.autotunnel.TunnelAutoTunnelScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.main.config.ConfigScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.main.sort.SortScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.main.splittunnel.SplitTunnelScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.main.tunneloptions.TunnelOptionsScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.pin.PinLockScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.SettingsScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance.AppearanceScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance.display.DisplayScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance.language.LanguageScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.dns.DnsSettingsScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.logs.LogsScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.monitoring.TunnelMonitoringScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.proxy.ProxySettingsScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.system.SystemFeaturesScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.support.SupportScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.support.license.LicenseScreen
import com.zaneschepke.wireguardautotunnel.ui.theme.AlertRed
import com.zaneschepke.wireguardautotunnel.ui.theme.OffWhite
import com.zaneschepke.wireguardautotunnel.ui.theme.WireguardAutoTunnelTheme
import com.zaneschepke.wireguardautotunnel.util.extensions.isRunningOnTv
import com.zaneschepke.wireguardautotunnel.util.extensions.restartApp
import com.zaneschepke.wireguardautotunnel.util.extensions.showToast
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent
import dagger.hilt.android.AndroidEntryPoint
import de.raphaelebner.roomdatabasebackup.core.RoomBackup
import java.util.Locale
import javax.inject.Inject
import kotlin.system.exitProcess
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var appStateRepository: AppStateRepository

    @Inject lateinit var tunnelManager: TunnelManager

    @Inject lateinit var networkMonitor: NetworkMonitor

    @Inject @IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher

    @Inject @MainDispatcher lateinit var mainDispatcher: CoroutineDispatcher

    @Inject lateinit var appDatabase: AppDatabase

    private var lastLocationPermissionState: Boolean? = null

    private lateinit var roomBackup: RoomBackup

    val REQUEST_CODE = 123

    @SuppressLint("BatteryLife")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.Companion.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.Companion.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        super.onCreate(savedInstanceState)
        roomBackup = RoomBackup(this)

        val viewModel by viewModels<AppViewModel>()

        installSplashScreen().apply {
            setKeepOnScreenCondition { !viewModel.appViewState.value.isAppReady }
        }

        setContent {
            val isTv = isRunningOnTv()
            val appUiState by viewModel.uiState.collectAsStateWithLifecycle()
            val appViewState by viewModel.appViewState.collectAsStateWithLifecycle()

            val navController = rememberNavController()
            val backStackEntry by navController.currentBackStackEntryAsState()
            val navBarState by
                currentNavBackStackEntryAsNavBarState(
                    navController,
                    backStackEntry,
                    viewModel,
                    appUiState,
                    appViewState,
                )
            val snackbar = remember { SnackbarHostState() }
            var showVpnPermissionDialog by remember { mutableStateOf(false) }
            var vpnPermissionDenied by remember { mutableStateOf(false) }

            val vpnActivity =
                rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult(),
                    onResult = {
                        if (it.resultCode != RESULT_OK) {
                            showVpnPermissionDialog = true
                            vpnPermissionDenied = true
                        } else {
                            vpnPermissionDenied = false
                            showVpnPermissionDialog = false
                        }
                    },
                )

            LaunchedEffect(appUiState.tunnels) {
                if (!appViewState.isAppReady) {
                    viewModel.handleEvent(AppEvent.AppReadyCheck(appUiState.tunnels))
                }
            }

            val batteryActivity =
                rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { _: ActivityResult ->
                    viewModel.handleEvent(AppEvent.SetBatteryOptimizeDisableShown)
                }

            with(appViewState) {
                LaunchedEffect(isConfigChanged) {
                    if (isConfigChanged) {
                        Intent(this@MainActivity, MainActivity::class.java).also {
                            startActivity(it)
                            exitProcess(0)
                        }
                    }
                }
                LaunchedEffect(errorMessage) {
                    errorMessage?.let {
                        snackbar.showSnackbar(it.asString(this@MainActivity))
                        viewModel.handleEvent(AppEvent.MessageShown)
                    }
                }
                LaunchedEffect(popBackStack) {
                    if (popBackStack) {
                        navController.popBackStack()
                        viewModel.handleEvent(AppEvent.PopBackStack(false))
                    }
                }
                LaunchedEffect(requestVpnPermission) {
                    if (requestVpnPermission) {
                        if (!vpnPermissionDenied) {
                            vpnActivity.launch(VpnService.prepare(this@MainActivity))
                        } else {
                            showVpnPermissionDialog = true
                        }
                        viewModel.handleEvent(AppEvent.VpnPermissionRequested)
                    }
                }
                LaunchedEffect(requestBatteryPermission) {
                    if (requestBatteryPermission) {
                        batteryActivity.launch(
                            Intent().apply {
                                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                data = "package:${this@MainActivity.packageName}".toUri()
                            }
                        )
                    }
                }
            }

            CompositionLocalProvider(LocalIsAndroidTV provides isTv) {
                CompositionLocalProvider(LocalNavController provides navController) {
                    WireguardAutoTunnelTheme(theme = appUiState.appState.theme) {
                        VpnDeniedDialog(
                            showVpnPermissionDialog,
                            onDismiss = {
                                showVpnPermissionDialog = false
                                vpnPermissionDenied = false
                            },
                        )

                        Box(modifier = Modifier.fillMaxSize()) {
                            // Top banner if in locked down mode
                            if (appUiState.appSettings.appMode == AppMode.LOCK_DOWN) {
                                AppAlertBanner(
                                    stringResource(R.string.locked_down)
                                        .uppercase(Locale.getDefault()),
                                    OffWhite,
                                    AlertRed,
                                    modifier =
                                        Modifier.fillMaxWidth().zIndex(2f), // Draw above everything
                                )
                            }

                            Scaffold(
                                modifier =
                                    Modifier.pointerInput(Unit) {
                                        detectTapGestures {
                                            viewModel.handleEvent(AppEvent.ClearSelectedTunnels)
                                        }
                                    },
                                snackbarHost = {
                                    SnackbarHost(snackbar) { snackbarData: SnackbarData ->
                                        CustomSnackBar(
                                            snackbarData.visuals.message,
                                            isRtl = false,
                                            containerColor =
                                                MaterialTheme.colorScheme.surfaceColorAtElevation(
                                                    2.dp
                                                ),
                                        )
                                    }
                                },
                                topBar = { DynamicTopAppBar(navBarState) },
                                bottomBar = {
                                    AnimatedVisibility(
                                        visible = navBarState.showBottom,
                                        enter = slideInVertically(initialOffsetY = { it }),
                                        exit = slideOutVertically(targetOffsetY = { it }),
                                    ) {
                                        BottomNavbar(appUiState = appUiState)
                                    }
                                },
                            ) { padding ->
                                Box(
                                    modifier =
                                        Modifier.fillMaxSize()
                                            .background(MaterialTheme.colorScheme.surface)
                                            .padding(padding)
                                            .consumeWindowInsets(padding)
                                            .imePadding()
                                ) {
                                    NavHost(
                                        navController,
                                        startDestination =
                                            (if (appUiState.appState.isPinLockEnabled) Route.Lock
                                            else Route.Main),
                                    ) {
                                        composable<Route.Main> {
                                            MainScreen(appUiState, appViewState, viewModel)
                                        }
                                        composable<Route.Settings> {
                                            SettingsScreen(appUiState, appViewState, viewModel)
                                        }
                                        composable<Route.LocationDisclosure> {
                                            LocationDisclosureScreen(viewModel)
                                        }
                                        composable<Route.AutoTunnel> {
                                            AutoTunnelScreen(appUiState, viewModel)
                                        }
                                        composable<Route.Appearance> { AppearanceScreen() }
                                        composable<Route.Language> {
                                            LanguageScreen(appUiState, viewModel)
                                        }
                                        composable<Route.Display> {
                                            DisplayScreen(appUiState, viewModel)
                                        }
                                        composable<Route.Support> {
                                            SupportScreen(appViewModel = viewModel)
                                        }
                                        composable<Route.License> { LicenseScreen() }
                                        composable<Route.AutoTunnelAdvanced> {
                                            AutoTunnelAdvancedScreen(appUiState, viewModel)
                                        }
                                        composable<Route.WifiDetectionMethod> {
                                            WifiDetectionMethodScreen(appUiState, viewModel)
                                        }
                                        composable<Route.Logs> {
                                            LogsScreen(appViewState, viewModel)
                                        }
                                        composable<Route.Config> { backStack ->
                                            val args = backStack.toRoute<Route.Config>()
                                            val config =
                                                appUiState.tunnels.firstOrNull { it.id == args.id }
                                            ConfigScreen(config, appUiState, viewModel)
                                        }
                                        composable<Route.TunnelOptions> { backStack ->
                                            val args = backStack.toRoute<Route.TunnelOptions>()
                                            appUiState.tunnels
                                                .firstOrNull { it.id == args.id }
                                                ?.let { config ->
                                                    TunnelOptionsScreen(
                                                        config,
                                                        viewModel,
                                                        appViewState,
                                                        appUiState.appSettings,
                                                    )
                                                }
                                        }
                                        composable<Route.Lock> { PinLockScreen(viewModel) }
                                        composable<Route.SplitTunnel> {
                                            SplitTunnelScreen(viewModel)
                                        }
                                        composable<Route.TunnelAutoTunnel> { backStack ->
                                            val args = backStack.toRoute<Route.TunnelOptions>()
                                            appUiState.tunnels
                                                .firstOrNull { it.id == args.id }
                                                ?.let {
                                                    TunnelAutoTunnelScreen(
                                                        it,
                                                        appUiState.appSettings,
                                                        viewModel,
                                                    )
                                                }
                                        }
                                        composable<Route.Sort> { SortScreen(appUiState, viewModel) }
                                        composable<Route.TunnelMonitoring> {
                                            TunnelMonitoringScreen(appUiState, viewModel)
                                        }
                                        composable<Route.ProxySettings> {
                                            ProxySettingsScreen(appUiState, viewModel)
                                        }
                                        composable<Route.SystemFeatures> {
                                            SystemFeaturesScreen(appUiState, viewModel)
                                        }
                                        composable<Route.Dns> {
                                            DnsSettingsScreen(appUiState, viewModel)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        WireGuardAutoTunnel.setUiActive(true)
        networkMonitor.checkPermissionsAndUpdateState()
    }

    override fun onPause() {
        super.onPause()
        WireGuardAutoTunnel.setUiActive(false)
    }

    fun performBackup() =
        lifecycleScope.launch(ioDispatcher) {
            roomBackup
                .database(appDatabase)
                .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_DIALOG)
                .enableLogDebug(true)
                .maxFileCount(5)
                .apply {
                    onCompleteListener { success, message, exitCode ->
                        lifecycleScope.launch(mainDispatcher) {
                            if (success) {
                                showToast(
                                    getString(
                                        R.string.backup_success,
                                        getString(R.string.restarting_app),
                                    )
                                )
                                restartApp()
                            } else showToast(R.string.backup_failed)
                        }
                    }
                }
                .backup()
        }

    fun performRestore() =
        lifecycleScope.launch {
            roomBackup
                .database(appDatabase)
                .enableLogDebug(true)
                .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_DIALOG)
                .apply {
                    onCompleteListener { success, message, exitCode ->
                        lifecycleScope.launch(mainDispatcher) {
                            if (success) {
                                showToast(
                                    getString(
                                        R.string.restore_success,
                                        getString(R.string.restarting_app),
                                    )
                                )
                                restartApp()
                            } else showToast(R.string.restore_failed)
                        }
                    }
                }
                .restore()
        }
}
