package com.zaneschepke.wireguardautotunnel.ui.navigation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ActionIconButton
import com.zaneschepke.wireguardautotunnel.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.ui.sideeffect.LocalSideEffect
import com.zaneschepke.wireguardautotunnel.ui.state.NavbarState
import com.zaneschepke.wireguardautotunnel.viewmodel.SharedAppViewModel

@Composable
fun NavHostController.currentBackStackEntryAsNavbarState(
    sharedViewModel: SharedAppViewModel
): State<NavbarState> {
    val sharedState by sharedViewModel.container.stateFlow.collectAsStateWithLifecycle()
    val backStackEntry by currentBackStackEntryAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val route =
        remember(backStackEntry) {
            backStackEntry?.destination?.route?.let {
                when (it.substringBefore("?").substringBefore("/").substringAfterLast(".")) {
                    Route.Support::class.simpleName -> backStackEntry?.toRoute<Route.Support>()
                    Route.Lock::class.simpleName -> backStackEntry?.toRoute<Route.Lock>()
                    Route.License::class.simpleName -> backStackEntry?.toRoute<Route.License>()
                    Route.Logs::class.simpleName -> backStackEntry?.toRoute<Route.Logs>()
                    Route.Appearance::class.simpleName ->
                        backStackEntry?.toRoute<Route.Appearance>()
                    Route.Language::class.simpleName -> backStackEntry?.toRoute<Route.Language>()
                    Route.Display::class.simpleName -> backStackEntry?.toRoute<Route.Display>()
                    Route.Tunnels::class.simpleName -> backStackEntry?.toRoute<Route.Tunnels>()
                    Route.TunnelOptions::class.simpleName ->
                        backStackEntry?.toRoute<Route.TunnelOptions>()
                    Route.Config::class.simpleName -> backStackEntry?.toRoute<Route.Config>()
                    Route.SplitTunnel::class.simpleName ->
                        backStackEntry?.toRoute<Route.SplitTunnel>()
                    Route.TunnelAutoTunnel::class.simpleName ->
                        backStackEntry?.toRoute<Route.TunnelAutoTunnel>()
                    Route.Sort::class.simpleName -> backStackEntry?.toRoute<Route.Sort>()
                    Route.Settings::class.simpleName -> backStackEntry?.toRoute<Route.Settings>()
                    Route.TunnelMonitoring::class.simpleName ->
                        backStackEntry?.toRoute<Route.TunnelMonitoring>()
                    Route.SystemFeatures::class.simpleName ->
                        backStackEntry?.toRoute<Route.SystemFeatures>()
                    Route.Dns::class.simpleName -> backStackEntry?.toRoute<Route.Dns>()
                    Route.ProxySettings::class.simpleName ->
                        backStackEntry?.toRoute<Route.ProxySettings>()
                    Route.AutoTunnel::class.simpleName ->
                        backStackEntry?.toRoute<Route.AutoTunnel>()
                    Route.AdvancedAutoTunnel::class.simpleName ->
                        backStackEntry?.toRoute<Route.AdvancedAutoTunnel>()
                    Route.WifiDetectionMethod::class.simpleName ->
                        backStackEntry?.toRoute<Route.WifiDetectionMethod>()
                    Route.LocationDisclosure::class.simpleName ->
                        backStackEntry?.toRoute<Route.LocationDisclosure>()
                    else -> null
                }
            }
        }

    return produceState(initialValue = NavbarState(), route, sharedState.topNavActions) {
        value =
            when (route) {
                Route.AdvancedAutoTunnel ->
                    NavbarState(
                        showBottomItems = true,
                        topTitle = { Text(stringResource(R.string.advanced_settings)) },
                    )
                Route.Appearance ->
                    NavbarState(
                        showBottomItems = true,
                        topTitle = { Text(stringResource(R.string.appearance)) },
                    )
                Route.AutoTunnel ->
                    NavbarState(
                        showBottomItems = true,
                        topTitle = { Text(stringResource(R.string.auto_tunnel)) },
                    )
                is Route.Config -> {
                    val tunnel = sharedState.tunnels.find { it.id == route.id }
                    NavbarState(
                        showBottomItems = true,
                        topTitle = {
                            val title = tunnel?.name ?: stringResource(R.string.new_tunnel)
                            Text(title)
                        },
                        topTrailing = {
                            ActionIconButton(Icons.Rounded.Save, R.string.save) {
                                keyboardController?.hide()
                                sharedViewModel.postSideEffect(LocalSideEffect.SaveChanges)
                            }
                        },
                    )
                }
                Route.Display ->
                    NavbarState(
                        showBottomItems = true,
                        topTitle = { Text(stringResource(R.string.display_theme)) },
                    )
                Route.Dns ->
                    NavbarState(
                        showBottomItems = true,
                        topTitle = { Text(stringResource(R.string.dns_settings)) },
                    )
                Route.Language ->
                    NavbarState(
                        showBottomItems = true,
                        topTitle = { Text(stringResource(R.string.language)) },
                    )
                Route.License ->
                    NavbarState(
                        showBottomItems = true,
                        topTitle = { Text(stringResource(R.string.licenses)) },
                    )
                Route.LocationDisclosure -> NavbarState(showBottomItems = true)
                Route.Lock -> NavbarState(showBottomItems = false)
                Route.Logs ->
                    NavbarState(
                        showBottomItems = false,
                        removeBottom = true,
                        topTitle = { Text(stringResource(R.string.logs)) },
                        topTrailing = {
                            ActionIconButton(Icons.Rounded.Menu, R.string.quick_actions) {
                                sharedViewModel.postSideEffect(LocalSideEffect.Sheet.LoggerActions)
                            }
                        },
                    )
                Route.ProxySettings ->
                    NavbarState(
                        showBottomItems = true,
                        topTitle = { Text(stringResource(R.string.proxy_settings)) },
                        topTrailing = {
                            ActionIconButton(Icons.Rounded.Save, R.string.save) {
                                keyboardController?.hide()
                                sharedViewModel.postSideEffect(LocalSideEffect.SaveChanges)
                            }
                        },
                    )
                Route.Settings ->
                    NavbarState(
                        showBottomItems = true,
                        topTitle = { Text(stringResource(R.string.settings)) },
                        topTrailing = {
                            ActionIconButton(
                                Icons.Rounded.SettingsBackupRestore,
                                R.string.quick_actions,
                            ) {
                                sharedViewModel.postSideEffect(LocalSideEffect.Sheet.BackupApp)
                            }
                        },
                    )
                Route.Sort ->
                    NavbarState(
                        showBottomItems = true,
                        topTitle = { Text(stringResource(R.string.sort)) },
                        topTrailing = {
                            Row {
                                ActionIconButton(Icons.Rounded.SortByAlpha, R.string.sort) {
                                    sharedViewModel.postSideEffect(LocalSideEffect.Sort)
                                }
                                ActionIconButton(Icons.Rounded.Save, R.string.save) {
                                    sharedViewModel.postSideEffect(LocalSideEffect.SaveChanges)
                                }
                            }
                        },
                    )
                is Route.SplitTunnel -> {
                    val tunnel = sharedState.tunnels.find { it.id == route.id }
                    NavbarState(
                        topTitle = { Text(tunnel?.name ?: "") },
                        topTrailing = {
                            ActionIconButton(Icons.Rounded.Save, R.string.save) {
                                sharedViewModel.postSideEffect(LocalSideEffect.SaveChanges)
                            }
                        },
                        showBottomItems = true,
                    )
                }
                Route.Support ->
                    NavbarState(
                        topTitle = { Text(stringResource(R.string.support)) },
                        showBottomItems = true,
                    )
                Route.SystemFeatures ->
                    NavbarState(
                        topTitle = { Text(stringResource(R.string.android_integrations)) },
                        showBottomItems = true,
                    )
                is Route.TunnelAutoTunnel -> {
                    val tunnel = sharedState.tunnels.find { it.id == route.id }
                    NavbarState(showBottomItems = true, topTitle = { Text(tunnel?.name ?: "") })
                }
                Route.TunnelMonitoring ->
                    NavbarState(
                        topTitle = { Text(stringResource(R.string.tunnel_monitoring)) },
                        showBottomItems = true,
                    )
                is Route.TunnelOptions -> {
                    val tunnel = sharedState.tunnels.find { it.id == route.id }
                    NavbarState(
                        showBottomItems = true,
                        topTitle = { Text(tunnel?.name ?: "") },
                        topTrailing = {
                            Row {
                                ActionIconButton(Icons.Rounded.QrCode2, R.string.show_qr) {
                                    sharedViewModel.postSideEffect(LocalSideEffect.Modal.QR)
                                }
                                ActionIconButton(Icons.Rounded.Edit, R.string.edit_tunnel) {
                                    navigate(Route.Config(route.id))
                                }
                            }
                        },
                    )
                }
                Route.Tunnels ->
                    NavbarState(
                        topTitle = { Text(stringResource(R.string.tunnels)) },
                        topTrailing =
                            sharedState.topNavActions
                                ?: {
                                    Row {
                                        ActionIconButton(
                                            Icons.AutoMirrored.Rounded.Sort,
                                            R.string.sort,
                                        ) {
                                            navigate(Route.Sort)
                                        }
                                        ActionIconButton(Icons.Rounded.Add, R.string.add_tunnel) {
                                            sharedViewModel.postSideEffect(
                                                LocalSideEffect.Sheet.ImportTunnels
                                            )
                                        }
                                    }
                                },
                        showBottomItems = true,
                    )
                Route.WifiDetectionMethod ->
                    NavbarState(
                        topTitle = { Text(stringResource(R.string.wifi_detection_method)) },
                        showBottomItems = true,
                    )
                Route.TunnelsGraph,
                Route.SettingsGraph,
                Route.AutoTunnelGraph,
                Route.SupportGraph,
                null -> NavbarState()
            }
    }
}
