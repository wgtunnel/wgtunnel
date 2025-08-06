package com.zaneschepke.wireguardautotunnel.ui.navigation.components

import android.os.Build
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.toRoute
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.navigation.isCurrentRoute
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.ui.state.AppViewState
import com.zaneschepke.wireguardautotunnel.ui.state.NavBarState
import com.zaneschepke.wireguardautotunnel.ui.theme.iconSize
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent
import com.zaneschepke.wireguardautotunnel.viewmodel.event.UiEvent

@Composable
fun currentNavBackStackEntryAsNavBarState(
    navController: NavController,
    backStackEntry: NavBackStackEntry?,
    viewModel: AppViewModel,
    uiState: AppUiState,
    appViewState: AppViewState,
): State<NavBarState> {
    fun isActiveSelected() =
        uiState.activeTunnels.any { active ->
            appViewState.selectedTunnels.any { it.id == active.key.id }
        }

    @Composable
    fun ActionIconButton(icon: ImageVector, labelRes: Int, onClick: () -> Unit) {
        IconButton(onClick = onClick) {
            Icon(
                icon,
                contentDescription = stringResource(labelRes),
                modifier = Modifier.size(iconSize),
            )
        }
    }

    @Composable
    fun TunnelActionBar() {
        val selectedCount = appViewState.selectedTunnels.size
        val showDelete = !isActiveSelected()

        Row {
            if (selectedCount == 0) {
                val showSort = remember(uiState.tunnels) { uiState.tunnels.size > 1 }
                if (showSort)
                    ActionIconButton(Icons.AutoMirrored.Rounded.Sort, R.string.sort) {
                        navController.navigate(Route.Sort)
                    }
                ActionIconButton(Icons.Rounded.Add, R.string.add_tunnel) {
                    viewModel.handleEvent(
                        AppEvent.SetBottomSheet(AppViewState.BottomSheet.IMPORT_TUNNELS)
                    )
                }
                return@Row
            }
            ActionIconButton(Icons.Rounded.SelectAll, R.string.select_all) {
                viewModel.handleEvent(AppEvent.ToggleSelectAllTunnels)
            }
            // due to permissions, and SAF issues on TV, not support less than Android 10 on
            // Android TV for file exports
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ActionIconButton(Icons.Rounded.Download, R.string.download) {
                    viewModel.handleEvent(
                        AppEvent.SetBottomSheet(AppViewState.BottomSheet.EXPORT_TUNNELS)
                    )
                }
            }

            if (selectedCount == 1) {
                ActionIconButton(Icons.Rounded.CopyAll, R.string.copy) {
                    viewModel.handleEvent(AppEvent.CopySelectedTunnel)
                }
            }

            if (showDelete) {
                ActionIconButton(Icons.Rounded.Delete, R.string.delete_tunnel) {
                    viewModel.handleEvent(AppEvent.SetShowModal(AppViewState.ModalType.DELETE))
                }
            }
        }
    }

    return produceState(
        initialValue = NavBarState(),
        key1 = backStackEntry,
        key2 = uiState,
        key3 = appViewState,
    ) {
        value =
            when {
                backStackEntry.isCurrentRoute(Route.Main::class) -> {
                    NavBarState(
                        topTitle = { Text(stringResource(R.string.tunnels)) },
                        topTrailing = { TunnelActionBar() },
                        route = Route.Main,
                    )
                }

                backStackEntry.isCurrentRoute(Route.AutoTunnel::class) -> {

                    NavBarState(
                        topTitle = { Text(stringResource(R.string.auto_tunnel)) },
                        route = Route.AutoTunnel,
                    )
                }

                backStackEntry.isCurrentRoute(Route.Logs::class) -> {
                    NavBarState(
                        showBottom = false,
                        topTitle = { Text(stringResource(R.string.logs)) },
                        topTrailing = {
                            ActionIconButton(Icons.Rounded.Menu, R.string.quick_actions) {
                                viewModel.handleEvent(
                                    AppEvent.SetBottomSheet(AppViewState.BottomSheet.LOGS)
                                )
                            }
                        },
                        route = Route.Logs,
                    )
                }

                backStackEntry.isCurrentRoute(Route.Settings::class) ->
                    NavBarState(
                        topTitle = { Text(stringResource(R.string.settings)) },
                        route = Route.Settings,
                    )

                backStackEntry.isCurrentRoute(Route.Appearance::class) ->
                    NavBarState(
                        topTitle = { Text(stringResource(R.string.appearance)) },
                        route = Route.Appearance,
                    )

                backStackEntry.isCurrentRoute(Route.Language::class) ->
                    NavBarState(
                        topTitle = { Text(stringResource(R.string.language)) },
                        route = Route.Language,
                    )

                backStackEntry.isCurrentRoute(Route.Display::class) ->
                    NavBarState(
                        topTitle = { Text(stringResource(R.string.display_theme)) },
                        route = Route.Display,
                    )

                backStackEntry.isCurrentRoute(Route.TunnelMonitoring::class) ->
                    NavBarState(
                        topTitle = { Text(stringResource(R.string.tunnel_monitoring)) },
                        route = Route.TunnelMonitoring,
                    )

                backStackEntry.isCurrentRoute(Route.WifiDetectionMethod::class) ->
                    NavBarState(
                        topTitle = { Text(stringResource(R.string.wifi_detection_method)) },
                        route = Route.WifiDetectionMethod,
                    )

                backStackEntry.isCurrentRoute(Route.KillSwitch::class) ->
                    NavBarState(
                        topTitle = { Text(stringResource(R.string.kill_switch)) },
                        route = Route.KillSwitch,
                    )

                backStackEntry.isCurrentRoute(Route.Support::class) ->
                    NavBarState(
                        topTitle = { Text(stringResource(R.string.support)) },
                        route = Route.Support,
                    )

                backStackEntry.isCurrentRoute(Route.Sort::class) -> {
                    NavBarState(
                        showTop = true,
                        showBottom = true,
                        topTitle = { Text(stringResource(R.string.sort)) },
                        topTrailing = {
                            Row {
                                ActionIconButton(Icons.Rounded.SortByAlpha, R.string.sort) {
                                    viewModel.handleUiEvent(UiEvent.SortTunnels)
                                }
                                ActionIconButton(Icons.Rounded.Save, R.string.save) {
                                    viewModel.handleEvent(AppEvent.InvokeScreenAction)
                                }
                            }
                        },
                        route = Route.Sort,
                    )
                }

                backStackEntry.isCurrentRoute(Route.License::class) -> {
                    NavBarState(
                        showTop = true,
                        showBottom = true,
                        topTitle = { Text(stringResource(R.string.licenses)) },
                        route = Route.License,
                    )
                }

                backStackEntry.isCurrentRoute(Route.AutoTunnelAdvanced::class) ||
                    backStackEntry.isCurrentRoute(Route.SettingsAdvanced::class) ->
                    NavBarState(
                        showTop = true,
                        showBottom = true,
                        topTitle = { Text(stringResource(R.string.advanced_settings)) },
                        route = Route.AutoTunnelAdvanced,
                    )

                backStackEntry.isCurrentRoute(Route.TunnelOptions::class) -> {
                    val args = backStackEntry?.toRoute<Route.TunnelOptions>()
                    val tunnel = uiState.tunnels.find { it.id == args?.id }

                    NavBarState(
                        showTop = true,
                        showBottom = true,
                        topTitle = { tunnel?.name?.let { Text(it) } },
                        topTrailing = {
                            Row {
                                ActionIconButton(Icons.Rounded.QrCode2, R.string.show_qr) {
                                    tunnel?.id?.let {
                                        viewModel.handleEvent(
                                            AppEvent.SetShowModal(AppViewState.ModalType.QR)
                                        )
                                    }
                                }
                                ActionIconButton(Icons.Rounded.Edit, R.string.edit_tunnel) {
                                    tunnel?.id?.let { navController.navigate(Route.Config(it)) }
                                }
                            }
                        },
                        route = args?.let { Route.TunnelOptions(it.id) },
                    )
                }

                backStackEntry.isCurrentRoute(Route.SplitTunnel::class) -> {
                    val args = backStackEntry?.toRoute<Route.SplitTunnel>()
                    val name = uiState.tunnels.find { it.id == args?.id }?.name

                    NavBarState(
                        showTop = true,
                        showBottom = true,
                        topTitle = { name?.let { Text(it) } },
                        topTrailing = {
                            ActionIconButton(Icons.Rounded.Save, R.string.save) {
                                viewModel.handleEvent(AppEvent.InvokeScreenAction)
                            }
                        },
                        route = args?.let { Route.SplitTunnel(it.id) },
                    )
                }

                backStackEntry.isCurrentRoute(Route.Config::class) -> {
                    val args = backStackEntry?.toRoute<Route.Config>()
                    val name = uiState.tunnels.find { it.id == args?.id }?.name

                    NavBarState(
                        showTop = true,
                        showBottom = true,
                        topTitle = { name?.let { Text(it) } },
                        topTrailing = {
                            ActionIconButton(Icons.Rounded.Save, R.string.save) {
                                viewModel.handleEvent(AppEvent.InvokeScreenAction)
                            }
                        },
                        route = args?.let { Route.Config(it.id) },
                    )
                }

                backStackEntry.isCurrentRoute(Route.TunnelAutoTunnel::class) -> {
                    val args = backStackEntry?.toRoute<Route.TunnelAutoTunnel>()
                    val name = uiState.tunnels.find { it.id == args?.id }?.name

                    NavBarState(
                        showTop = true,
                        showBottom = true,
                        topTitle = { name?.let { Text(it) } },
                        route = args?.let { Route.TunnelAutoTunnel(it.id) },
                    )
                }

                else -> NavBarState(showTop = false, showBottom = false)
            }
    }
}
