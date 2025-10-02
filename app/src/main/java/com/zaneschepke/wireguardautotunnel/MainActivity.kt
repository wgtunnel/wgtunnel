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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.*
import androidx.navigation3.scene.rememberSceneSetupNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.zaneschepke.networkmonitor.NetworkMonitor
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.data.AppDatabase
import com.zaneschepke.wireguardautotunnel.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.AppStateRepository
import com.zaneschepke.wireguardautotunnel.domain.sideeffect.GlobalSideEffect
import com.zaneschepke.wireguardautotunnel.ui.LocalBackStack
import com.zaneschepke.wireguardautotunnel.ui.LocalIsAndroidTV
import com.zaneschepke.wireguardautotunnel.ui.LocalSharedVm
import com.zaneschepke.wireguardautotunnel.ui.common.banner.AppAlertBanner
import com.zaneschepke.wireguardautotunnel.ui.common.dialog.VpnDeniedDialog
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.CustomSnackBar
import com.zaneschepke.wireguardautotunnel.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.ui.navigation.Tab
import com.zaneschepke.wireguardautotunnel.ui.navigation.components.BottomNavbar
import com.zaneschepke.wireguardautotunnel.ui.navigation.components.DynamicTopAppBar
import com.zaneschepke.wireguardautotunnel.ui.navigation.components.currentRouteAsNavbarState
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
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.globals.TunnelGlobalsScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.logs.LogsScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.monitoring.TunnelMonitoringScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.system.SystemFeaturesScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.support.SupportScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.support.donate.DonateScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.support.donate.crypto.AddressesScreen
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
import java.util.*
import java.util.Map.entry
import javax.inject.Inject
import kotlinx.coroutines.launch
import xyz.teamgravity.pin_lock_compose.PinManager

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var appStateRepository: AppStateRepository
    @Inject lateinit var tunnelManager: TunnelManager
    @Inject lateinit var networkMonitor: NetworkMonitor
    @Inject lateinit var appDatabase: AppDatabase

    private lateinit var roomBackup: RoomBackup

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("BatteryLife")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
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
            val scope = rememberCoroutineScope()

            LaunchedEffect(appState.isAppLoaded) {
                if (appState.isAppLoaded) {
                    appState.locale.let { LocaleUtil.changeLocale(it) }
                }
            }

            val sharedState by viewModel.container.stateFlow.collectAsStateWithLifecycle()
            val snackbar = remember { SnackbarHostState() }
            var showVpnPermissionDialog by remember { mutableStateOf(false) }
            var vpnPermissionDenied by remember { mutableStateOf(false) }
            var requestingAppMode by remember {
                mutableStateOf<Pair<AppMode?, TunnelConf?>>(Pair(null, null))
            }

            val startingStack = buildList {
                add(Route.Tunnels)
                if (intent?.action == Intent.ACTION_APPLICATION_PREFERENCES) add(Route.Settings)
            }

            val backStack = rememberNavBackStack(*startingStack.toTypedArray())

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
                        GlobalSideEffect.PopBackStack -> backStack.removeLastOrNull()
                        GlobalSideEffect.RequestBatteryOptimizationDisabled ->
                            requestDisableBatteryOptimizations()

                        is GlobalSideEffect.RequestVpnPermission -> {
                            requestingAppMode = Pair(sideEffect.requestingMode, sideEffect.config)
                            vpnActivity.launch(VpnService.prepare(this@MainActivity))
                        }

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

            var showLock by remember {
                mutableStateOf(appState.pinLockEnabled && !appState.isAuthorized)
            }
            LaunchedEffect(appState.isAuthorized) { if (appState.isAuthorized) showLock = false }

            CompositionLocalProvider(
                LocalIsAndroidTV provides isTv,
                LocalSharedVm provides viewModel,
                LocalBackStack provides backStack,
            ) {
                WireguardAutoTunnelTheme(theme = appState.theme) {
                    VpnDeniedDialog(
                        showVpnPermissionDialog,
                        onDismiss = {
                            showVpnPermissionDialog = false
                            vpnPermissionDenied = false
                        },
                    )

                    if (showLock) {
                        PinManager.initialize(context = this@MainActivity)
                        PinLockScreen()
                    } else {
                        val currentRoute by remember {
                            derivedStateOf { backStack.lastOrNull() as? Route }
                        }
                        val currentTab by remember {
                            derivedStateOf { Tab.fromRoute(currentRoute ?: Route.Tunnels) }
                        }
                        val selectedCount by
                            rememberSaveable(sharedState.selectedTunnels) {
                                mutableIntStateOf(sharedState.selectedTunnels.size)
                            }

                        val navState by
                            currentRouteAsNavbarState(
                                viewModel,
                                currentRoute,
                                selectedCount,
                                backStack,
                            )

                        Box(modifier = Modifier.fillMaxSize()) {
                            if (appState.settings.appMode == AppMode.LOCK_DOWN) {
                                AppAlertBanner(
                                    stringResource(R.string.locked_down)
                                        .uppercase(Locale.getDefault()),
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
                                                MaterialTheme.colorScheme.surfaceColorAtElevation(
                                                    2.dp
                                                ),
                                        )
                                    }
                                },
                                topBar = { DynamicTopAppBar(navState) },
                                bottomBar = {
                                    if (navState.showBottomItems) {
                                        BottomNavbar(
                                            appState.isAutoTunnelActive,
                                            currentTab,
                                            onTabSelected = { tab ->
                                                backStack.popUpTo(tab.startRoute)
                                                if (backStack.last() != tab.startRoute) {
                                                    backStack.add(tab.startRoute)
                                                }
                                            },
                                        )
                                    }
                                },
                                modifier =
                                    Modifier.pointerInput(Unit) {
                                        detectTapGestures { viewModel.clearSelectedTunnels() }
                                    },
                            ) { padding ->
                                Column(
                                    modifier =
                                        Modifier.fillMaxSize()
                                            .background(MaterialTheme.colorScheme.surface)
                                            .padding(
                                                top = padding.calculateTopPadding().plus(8.dp),
                                                bottom = padding.calculateBottomPadding(),
                                            )
                                            .consumeWindowInsets(padding)
                                            .imePadding()
                                ) {
                                    NavDisplay(
                                        backStack = backStack,
                                        modifier = Modifier.fillMaxSize(),
                                        onBack = { backStack.removeLastOrNull() },
                                        entryDecorators =
                                            listOf(
                                                rememberSceneSetupNavEntryDecorator(),
                                                rememberSavedStateNavEntryDecorator(),
                                                rememberViewModelStoreNavEntryDecorator(),
                                            ),
                                        entryProvider =
                                            entryProvider {
                                                entry<Route.Lock> {
                                                    PinManager.initialize(
                                                        context = this@MainActivity
                                                    )
                                                    PinLockScreen()
                                                }
                                                entry<Route.Tunnels> { TunnelsScreen() }
                                                entry<Route.Sort> { SortScreen() }
                                                entry<Route.TunnelOptions> { key ->
                                                    TunnelOptionsScreen(key.id)
                                                }
                                                entry<Route.SplitTunnel> { key ->
                                                    SplitTunnelScreen(key.id)
                                                }
                                                entry<Route.TunnelAutoTunnel> { key ->
                                                    TunnelAutoTunnelScreen(key.id)
                                                }
                                                entry<Route.Config> { key -> ConfigScreen(key.id) }
                                                entry<Route.LocationDisclosure> {
                                                    LocationDisclosureScreen()
                                                }
                                                entry<Route.AutoTunnel> { AutoTunnelScreen() }
                                                entry<Route.AdvancedAutoTunnel> {
                                                    AutoTunnelAdvancedScreen()
                                                }
                                                entry<Route.WifiDetectionMethod> {
                                                    WifiDetectionMethodScreen()
                                                }
                                                entry<Route.Settings> { SettingsScreen() }
                                                entry<Route.TunnelMonitoring> {
                                                    TunnelMonitoringScreen()
                                                }
                                                entry<Route.SystemFeatures> {
                                                    SystemFeaturesScreen()
                                                }
                                                entry<Route.Dns> { DnsSettingsScreen() }
                                                entry<Route.TunnelGlobals> { key ->
                                                    TunnelGlobalsScreen(key.id)
                                                }
                                                entry<Route.ConfigGlobal> { key ->
                                                    ConfigScreen(key.id)
                                                }
                                                entry<Route.SplitTunnelGlobal> { key ->
                                                    SplitTunnelScreen(key.id)
                                                }
                                                entry<Route.ProxySettings> { ProxySettingsScreen() }
                                                entry<Route.Appearance> { AppearanceScreen() }
                                                entry<Route.Language> { LanguageScreen() }
                                                entry<Route.Display> { DisplayScreen() }
                                                entry<Route.Logs> { LogsScreen() }
                                                entry<Route.Support> { SupportScreen() }
                                                entry<Route.License> { LicenseScreen() }
                                                entry<Route.Donate> { DonateScreen() }
                                                entry<Route.Addresses> { AddressesScreen() }
                                            },
                                    )
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
