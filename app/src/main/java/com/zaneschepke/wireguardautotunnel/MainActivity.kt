package com.zaneschepke.wireguardautotunnel

import ProxySettingsScreen
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.zaneschepke.networkmonitor.NetworkMonitor
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.data.AppDatabase
import com.zaneschepke.wireguardautotunnel.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.AppStateRepository
import com.zaneschepke.wireguardautotunnel.domain.sideeffect.GlobalSideEffect
import com.zaneschepke.wireguardautotunnel.ui.LocalIsAndroidTV
import com.zaneschepke.wireguardautotunnel.ui.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.LocalSharedVm
import com.zaneschepke.wireguardautotunnel.ui.common.banner.AppAlertBanner
import com.zaneschepke.wireguardautotunnel.ui.common.dialog.VpnDeniedDialog
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.CustomSnackBar
import com.zaneschepke.wireguardautotunnel.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.ui.navigation.components.BottomNavbar
import com.zaneschepke.wireguardautotunnel.ui.navigation.components.DynamicTopAppBar
import com.zaneschepke.wireguardautotunnel.ui.navigation.components.currentBackStackEntryAsNavbarState
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.AutoTunnelScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.advanced.AutoTunnelAdvancedScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.detection.WifiDetectionMethodScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.disclosure.LocationDisclosureScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.pin.PinLockScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.SettingsScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance.AppearanceScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance.display.DisplayScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance.language.LanguageScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.dns.DnsSettingsScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.logs.LogsScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.monitoring.TunnelMonitoringScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.system.SystemFeaturesScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.support.SupportScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.support.license.LicenseScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.TunnelsScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.autotunnel.TunnelAutoTunnelScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.config.ConfigScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.sort.SortScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.splittunnel.SplitTunnelScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.tunneloptions.TunnelOptionsScreen
import com.zaneschepke.wireguardautotunnel.ui.theme.AlertRed
import com.zaneschepke.wireguardautotunnel.ui.theme.OffWhite
import com.zaneschepke.wireguardautotunnel.ui.theme.WireguardAutoTunnelTheme
import com.zaneschepke.wireguardautotunnel.util.LocaleUtil
import com.zaneschepke.wireguardautotunnel.util.extensions.*
import com.zaneschepke.wireguardautotunnel.viewmodel.*
import dagger.hilt.android.AndroidEntryPoint
import de.raphaelebner.roomdatabasebackup.core.RoomBackup
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.teamgravity.pin_lock_compose.PinManager
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var appStateRepository: AppStateRepository
    @Inject lateinit var tunnelManager: TunnelManager
    @Inject lateinit var networkMonitor: NetworkMonitor
    @Inject lateinit var appDatabase: AppDatabase

    private lateinit var roomBackup: RoomBackup

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

        val viewModel by viewModels<SharedAppViewModel>()

        installSplashScreen().apply {
            setKeepOnScreenCondition { !viewModel.container.stateFlow.value.isAppLoaded }
        }

        setContent {
            val context = LocalContext.current
            val isTv = isRunningOnTv()
            val appState by viewModel.container.stateFlow.collectAsStateWithLifecycle()
            val navController = rememberNavController()
            val scope = rememberCoroutineScope()

            var pinManagerInitialized by remember { mutableStateOf(false) }

            LaunchedEffect(appState.isAppLoaded) {
                if (appState.isAppLoaded) {
                    if (appState.pinLockEnabled && !pinManagerInitialized) {
                        PinManager.initialize(this@MainActivity)
                        pinManagerInitialized = true
                    }
                    appState.locale.let { LocaleUtil.changeLocale(it) }
                }
            }

            val navState by navController.currentBackStackEntryAsNavbarState(viewModel)
            val snackbar = remember { SnackbarHostState() }
            var showVpnPermissionDialog by remember { mutableStateOf(false) }
            var vpnPermissionDenied by remember { mutableStateOf(false) }
            var requestingAppMode by remember {
                mutableStateOf<Pair<AppMode?, TunnelConf?>>(Pair(null, null))
            }

            LaunchedEffect(navState) { Timber.d("New navbar state $navState") }

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
                            val (appMode, config) = requestingAppMode
                            when (appMode) {
                                AppMode.VPN -> if (config != null) viewModel.startTunnel(config)
                                AppMode.LOCK_DOWN -> viewModel.setAppMode(AppMode.LOCK_DOWN)
                                else -> Unit
                            }
                        }
                        requestingAppMode = Pair(null, null)
                    },
                )

            val batteryActivity =
                rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { _: ActivityResult ->
                    viewModel.disableBatteryOptimizationsShown()
                }

            fun requestDisableBatteryOptimizations() {
                batteryActivity.launch(
                    Intent().apply {
                        action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                        data = "package:${this@MainActivity.packageName}".toUri()
                    }
                )
            }

            LaunchedEffect(Unit) {
                viewModel.globalSideEffect.collect { sideEffect ->
                    when (sideEffect) {
                        GlobalSideEffect.ConfigChanged -> restartApp()
                        GlobalSideEffect.PopBackStack -> navController.popBackStack()
                        GlobalSideEffect.RequestBatteryOptimizationDisabled ->
                            requestDisableBatteryOptimizations()
                        is GlobalSideEffect.RequestVpnPermission -> {
                            requestingAppMode = Pair(sideEffect.requestingMode, sideEffect.config)
                            vpnActivity.launch(VpnService.prepare(this@MainActivity))
                        }
                        is GlobalSideEffect.ShareFile -> context.launchShareFile(sideEffect.file)
                        is GlobalSideEffect.Snackbar ->
                            scope.launch {
                                snackbar.showSnackbar(sideEffect.message.asString(context))
                            }
                        is GlobalSideEffect.Toast ->
                            scope.launch { context.showToast(sideEffect.message.asString(context)) }
                        is GlobalSideEffect.LaunchUrl -> context.openWebUrl(sideEffect.url)
                        is GlobalSideEffect.InstallApk -> context.installApk(sideEffect.apk)
                    }
                }
            }

            if (!appState.isAppLoaded) return@setContent

            CompositionLocalProvider(
                LocalIsAndroidTV provides isTv,
                LocalSharedVm provides viewModel,
                LocalNavController provides navController,
            ) {
                WireguardAutoTunnelTheme(theme = appState.theme) {
                    VpnDeniedDialog(
                        showVpnPermissionDialog,
                        onDismiss = {
                            showVpnPermissionDialog = false
                            vpnPermissionDenied = false
                        },
                    )

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (appState.settings.appMode == AppMode.LOCK_DOWN) {
                            AppAlertBanner(
                                stringResource(R.string.locked_down).uppercase(Locale.getDefault()),
                                OffWhite,
                                AlertRed,
                                modifier = Modifier.fillMaxWidth().zIndex(2f),
                            )
                        }

                        Scaffold(
                            snackbarHost = {
                                SnackbarHost(snackbar) { snackbarData ->
                                    CustomSnackBar(
                                        snackbarData.visuals.message,
                                        isRtl = false,
                                        containerColor =
                                            MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                                    )
                                }
                            },
                            topBar = { DynamicTopAppBar(navState) },
                            bottomBar = {
                                BottomNavbar(appState.isAutoTunnelActive, navState, navController)
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
                                    navController = navController,
                                    startDestination =
                                        if (appState.pinLockEnabled && !appState.isAuthorized)
                                            Route.Lock
                                        else Route.TunnelsGraph,
                                ) {
                                    composable<Route.Lock> { PinLockScreen() }
                                    navigation<Route.TunnelsGraph>(
                                        startDestination = Route.Tunnels
                                    ) {
                                        composable<Route.Tunnels> {
                                            val viewModel =
                                                it.sharedViewModel<TunnelsViewModel>(navController)
                                            TunnelsScreen(viewModel)
                                        }
                                        composable<Route.Sort> {
                                            val viewModel =
                                                it.sharedViewModel<TunnelsViewModel>(navController)
                                            SortScreen(viewModel)
                                        }
                                        composable<Route.TunnelOptions> { backStackEntry ->
                                            val args = backStackEntry.toRoute<Route.TunnelOptions>()
                                            val viewModel =
                                                backStackEntry.sharedViewModel<TunnelsViewModel>(
                                                    navController
                                                )
                                            TunnelOptionsScreen(args.id, viewModel)
                                        }
                                        composable<Route.SplitTunnel> { backStackEntry ->
                                            val args = backStackEntry.toRoute<Route.SplitTunnel>()
                                            SplitTunnelScreen(args.id)
                                        }
                                        composable<Route.TunnelAutoTunnel> { backStackEntry ->
                                            val args =
                                                backStackEntry.toRoute<Route.TunnelAutoTunnel>()
                                            val viewModel =
                                                backStackEntry.sharedViewModel<TunnelsViewModel>(
                                                    navController
                                                )
                                            TunnelAutoTunnelScreen(args.id, viewModel)
                                        }
                                        composable<Route.Config> { backStackEntry ->
                                            val args = backStackEntry.toRoute<Route.Config>()
                                            val viewModel =
                                                backStackEntry.sharedViewModel<TunnelsViewModel>(
                                                    navController
                                                )
                                            ConfigScreen(args.id, viewModel)
                                        }
                                    }

                                    navigation<Route.AutoTunnelGraph>(
                                        startDestination =
                                            if (appState.isLocationDisclosureShown) Route.AutoTunnel
                                            else Route.LocationDisclosure
                                    ) {
                                        composable<Route.LocationDisclosure> {
                                            val viewModel =
                                                it.sharedViewModel<AutoTunnelViewModel>(
                                                    navController
                                                )
                                            LocationDisclosureScreen(viewModel)
                                        }
                                        composable<Route.AutoTunnel> {
                                            val viewModel =
                                                it.sharedViewModel<AutoTunnelViewModel>(
                                                    navController
                                                )
                                            AutoTunnelScreen(viewModel)
                                        }
                                        composable<Route.AdvancedAutoTunnel> {
                                            val viewModel =
                                                it.sharedViewModel<AutoTunnelViewModel>(
                                                    navController
                                                )
                                            AutoTunnelAdvancedScreen(viewModel)
                                        }
                                        composable<Route.WifiDetectionMethod> {
                                            val viewModel =
                                                it.sharedViewModel<AutoTunnelViewModel>(
                                                    navController
                                                )
                                            WifiDetectionMethodScreen(viewModel)
                                        }
                                    }

                                    navigation<Route.SettingsGraph>(
                                        startDestination = Route.Settings
                                    ) {
                                        composable<Route.Settings> {
                                            val viewModel =
                                                it.sharedViewModel<SettingsViewModel>(navController)
                                            SettingsScreen(viewModel)
                                        }
                                        composable<Route.TunnelMonitoring> {
                                            val viewModel =
                                                it.sharedViewModel<SettingsViewModel>(navController)
                                            TunnelMonitoringScreen(viewModel)
                                        }
                                        composable<Route.SystemFeatures> {
                                            val viewModel =
                                                it.sharedViewModel<SettingsViewModel>(navController)
                                            SystemFeaturesScreen(viewModel)
                                        }
                                        composable<Route.Dns> {
                                            val viewModel =
                                                it.sharedViewModel<SettingsViewModel>(navController)
                                            DnsSettingsScreen(viewModel)
                                        }
                                        composable<Route.ProxySettings> { ProxySettingsScreen() }
                                        composable<Route.Appearance> { AppearanceScreen() }
                                        composable<Route.Language> { LanguageScreen() }
                                        composable<Route.Display> { DisplayScreen() }
                                        composable<Route.Logs> { LogsScreen() }
                                    }

                                    navigation<Route.SupportGraph>(
                                        startDestination = Route.Support
                                    ) {
                                        composable<Route.Support> {
                                            val viewModel =
                                                it.sharedViewModel<SupportViewModel>(navController)
                                            SupportScreen(viewModel)
                                        }
                                        composable<Route.License> { LicenseScreen() }
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
        lifecycleScope.launch {
            roomBackup
                .database(appDatabase)
                .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_DIALOG)
                .enableLogDebug(true)
                .maxFileCount(5)
                .apply {
                    onCompleteListener { success, _, _ ->
                        lifecycleScope.launch {
                            if (success) {
                                showToast(
                                    getString(
                                        R.string.backup_success,
                                        getString(R.string.restarting_app),
                                    )
                                )
                                restartApp()
                            } else {
                                showToast(R.string.backup_failed)
                            }
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
                    onCompleteListener { success, _, _ ->
                        lifecycleScope.launch {
                            if (success) {
                                showToast(
                                    getString(
                                        R.string.restore_success,
                                        getString(R.string.restarting_app),
                                    )
                                )
                                restartApp()
                            } else {
                                showToast(R.string.restore_failed)
                            }
                        }
                    }
                }
                .restore()
        }
}
