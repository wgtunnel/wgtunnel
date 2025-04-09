package com.zaneschepke.wireguardautotunnel

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.zaneschepke.networkmonitor.NetworkMonitor
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.repository.AppStateRepository
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.dialog.VpnDeniedDialog
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.BottomNavItem
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.CustomBottomNavbar
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.DynamicTopAppBar
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.currentNavBackStackEntryAsNavBarState
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.CustomSnackBar
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.AutoTunnelScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.advanced.AutoTunnelAdvancedScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.main.MainScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.main.autotunnel.TunnelAutoTunnelScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.main.config.ConfigScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.main.scanner.ScannerScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.main.splittunnel.SplitTunnelScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.main.tunneloptions.TunnelOptionsScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.pin.PinLockScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.SettingsScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.advanced.SettingsAdvancedScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance.AppearanceScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance.display.DisplayScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance.language.LanguageScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.disclosure.LocationDisclosureScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.killswitch.KillSwitchScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.support.SupportScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.support.logs.LogsScreen
import com.zaneschepke.wireguardautotunnel.ui.theme.WireguardAutoTunnelTheme
import com.zaneschepke.wireguardautotunnel.util.extensions.goFromRoot
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.system.exitProcess
import org.amnezia.awg.backend.GoBackend.VpnService
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var appStateRepository: AppStateRepository

    @Inject lateinit var tunnelManager: TunnelManager

    @Inject lateinit var networkMonitor: NetworkMonitor

    private var lastLocationPermissionState: Boolean? = null

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

        val viewModel by viewModels<AppViewModel>()

        installSplashScreen().apply {
            setKeepOnScreenCondition { !viewModel.appViewState.value.isAppReady }
        }

        setContent {
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
                LaunchedEffect(appUiState.activeTunnels) {
                    appUiState.activeTunnels.mapNotNull { (tunnelConf, tunnelState) ->
                        (tunnelState.status as? TunnelStatus.Error)?.let { error ->
                            val message = error.error.toStringRes()
                            val context = this@MainActivity
                            snackbar.showSnackbar(
                                context.getString(
                                    R.string.tunnel_error_template,
                                    context.getString(message),
                                )
                            )
                            viewModel.handleEvent(AppEvent.ClearTunnelError(tunnelConf))
                        }
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
                                data = Uri.parse("package:${this@MainActivity.packageName}")
                            }
                        )
                    }
                }
            }

            CompositionLocalProvider(LocalNavController provides navController) {
                WireguardAutoTunnelTheme(theme = appUiState.appState.theme) {
                    VpnDeniedDialog(
                        showVpnPermissionDialog,
                        onDismiss = { showVpnPermissionDialog = false },
                    )

                    Scaffold(
                        modifier =
                            Modifier.pointerInput(Unit) {
                                detectTapGestures {
                                    viewModel.handleEvent(AppEvent.SetSelectedTunnel(null))
                                }
                            },
                        snackbarHost = {
                            SnackbarHost(snackbar) { snackbarData: SnackbarData ->
                                CustomSnackBar(
                                    snackbarData.visuals.message,
                                    isRtl = false,
                                    containerColor =
                                        MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
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
                                CustomBottomNavbar(
                                    listOf(
                                        BottomNavItem(
                                            name = stringResource(R.string.tunnels),
                                            route = Route.Main,
                                            icon = Icons.Rounded.Home,
                                            onClick = { navController.goFromRoot(Route.Main) },
                                        ),
                                        BottomNavItem(
                                            name = stringResource(R.string.auto_tunnel),
                                            route = Route.AutoTunnel,
                                            icon = Icons.Rounded.Bolt,
                                            onClick = {
                                                val route =
                                                    if (
                                                        appUiState.appState
                                                            .isLocationDisclosureShown
                                                    )
                                                        Route.AutoTunnel
                                                    else Route.LocationDisclosure
                                                navController.goFromRoot(route)
                                            },
                                            active = appUiState.isAutoTunnelActive,
                                        ),
                                        BottomNavItem(
                                            name = stringResource(R.string.settings),
                                            route = Route.Settings,
                                            icon = Icons.Rounded.Settings,
                                            onClick = { navController.goFromRoot(Route.Settings) },
                                        ),
                                        BottomNavItem(
                                            name = stringResource(R.string.support),
                                            route = Route.Support,
                                            icon = Icons.Rounded.QuestionMark,
                                            onClick = { navController.goFromRoot(Route.Support) },
                                        ),
                                    ),
                                    navBarState = navBarState,
                                )
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
                                composable<Route.SettingsAdvanced> {
                                    SettingsAdvancedScreen(appUiState, viewModel)
                                }
                                composable<Route.LocationDisclosure> {
                                    LocationDisclosureScreen(appUiState, viewModel)
                                }
                                composable<Route.AutoTunnel> {
                                    AutoTunnelScreen(appUiState, viewModel)
                                }
                                composable<Route.Appearance> { AppearanceScreen() }
                                composable<Route.Language> { LanguageScreen(appUiState, viewModel) }
                                composable<Route.Display> { DisplayScreen(appUiState, viewModel) }
                                composable<Route.Support> { SupportScreen() }
                                composable<Route.AutoTunnelAdvanced> {
                                    AutoTunnelAdvancedScreen(appUiState, viewModel)
                                }
                                composable<Route.Logs> { LogsScreen(appViewState, viewModel) }
                                composable<Route.Config> { backStack ->
                                    val args = backStack.toRoute<Route.Config>()
                                    val config = appUiState.tunnels.firstOrNull { it.id == args.id }
                                    ConfigScreen(config, viewModel)
                                }
                                composable<Route.TunnelOptions> { backStack ->
                                    val args = backStack.toRoute<Route.TunnelOptions>()
                                    appUiState.tunnels
                                        .firstOrNull { it.id == args.id }
                                        ?.let { config ->
                                            TunnelOptionsScreen(config, appUiState, viewModel)
                                        }
                                }
                                composable<Route.Lock> { PinLockScreen(viewModel) }
                                composable<Route.Scanner> { ScannerScreen(viewModel) }
                                composable<Route.KillSwitch> {
                                    KillSwitchScreen(appUiState, viewModel)
                                }
                                composable<Route.SplitTunnel> { SplitTunnelScreen(viewModel) }
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
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionAndNotify()
    }

    private fun checkPermissionAndNotify() {
        val hasLocation =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        if (lastLocationPermissionState != hasLocation) {
            Timber.d("Location permission changed to: $hasLocation")
            if (hasLocation) {
                networkMonitor.sendLocationPermissionsGrantedBroadcast()
            }
            lastLocationPermissionState = hasLocation
        }
    }
}
