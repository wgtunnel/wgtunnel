package com.zaneschepke.wireguardautotunnel.ui.navigation.components

import android.os.Build
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ActionIconButton
import com.zaneschepke.wireguardautotunnel.ui.navigation.NavController
import com.zaneschepke.wireguardautotunnel.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.ui.sideeffect.LocalSideEffect
import com.zaneschepke.wireguardautotunnel.ui.state.NavbarState
import com.zaneschepke.wireguardautotunnel.ui.state.SharedAppUiState
import com.zaneschepke.wireguardautotunnel.viewmodel.SharedAppViewModel

@Composable
fun currentRouteAsNavbarState(
    sharedState: SharedAppUiState,
    sharedViewModel: SharedAppViewModel,
    route: Route?,
    navController: NavController,
): State<NavbarState> {
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    return remember(route, sharedState) {
        derivedStateOf {
            when (route) {
                Route.AdvancedAutoTunnel ->
                    NavbarState(
                        topLeading = {
                            ActionIconButton(Icons.AutoMirrored.Rounded.ArrowBack, R.string.back) {
                                navController.pop()
                            }
                        },
                        showBottomItems = true,
                        topTitle = context.getString(R.string.advanced_settings),
                    )
                Route.Appearance ->
                    NavbarState(
                        topLeading = {
                            ActionIconButton(Icons.AutoMirrored.Rounded.ArrowBack, R.string.back) {
                                navController.pop()
                            }
                        },
                        showBottomItems = true,
                        topTitle = context.getString(R.string.appearance),
                    )
                Route.AutoTunnel ->
                    NavbarState(
                        showBottomItems = true,
                        topTitle =
                            if (!sharedState.isLocationDisclosureShown) null
                            else {
                                context.getString(R.string.auto_tunnel)
                            },
                    )
                Route.Display ->
                    NavbarState(
                        topLeading = {
                            ActionIconButton(Icons.AutoMirrored.Rounded.ArrowBack, R.string.back) {
                                navController.pop()
                            }
                        },
                        showBottomItems = true,
                        topTitle = context.getString(R.string.display_theme),
                    )
                Route.Dns ->
                    NavbarState(
                        topLeading = {
                            ActionIconButton(Icons.AutoMirrored.Rounded.ArrowBack, R.string.back) {
                                navController.pop()
                            }
                        },
                        showBottomItems = true,
                        topTitle = context.getString(R.string.dns_settings),
                    )
                Route.Language ->
                    NavbarState(
                        topLeading = {
                            ActionIconButton(Icons.AutoMirrored.Rounded.ArrowBack, R.string.back) {
                                navController.pop()
                            }
                        },
                        showBottomItems = true,
                        topTitle = context.getString(R.string.language),
                    )
                Route.License ->
                    NavbarState(
                        topLeading = {
                            ActionIconButton(Icons.AutoMirrored.Rounded.ArrowBack, R.string.back) {
                                navController.pop()
                            }
                        },
                        showBottomItems = true,
                        topTitle = context.getString(R.string.licenses),
                    )
                Route.LocationDisclosure -> NavbarState(showBottomItems = true)
                Route.Lock -> NavbarState(showBottomItems = false)
                Route.Logs ->
                    NavbarState(
                        topLeading = {
                            ActionIconButton(Icons.AutoMirrored.Rounded.ArrowBack, R.string.back) {
                                navController.pop()
                            }
                        },
                        showBottomItems = true,
                        topTitle = context.getString(R.string.logs),
                        topTrailing = {
                            ActionIconButton(Icons.Rounded.Menu, R.string.quick_actions) {
                                sharedViewModel.postSideEffect(LocalSideEffect.Sheet.LoggerActions)
                            }
                        },
                    )
                Route.ProxySettings ->
                    NavbarState(
                        topLeading = {
                            ActionIconButton(Icons.AutoMirrored.Rounded.ArrowBack, R.string.back) {
                                navController.pop()
                            }
                        },
                        showBottomItems = true,
                        topTitle = context.getString(R.string.proxy_settings),
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
                        topTitle = context.getString(R.string.settings),
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
                        topLeading = {
                            ActionIconButton(Icons.AutoMirrored.Rounded.ArrowBack, R.string.back) {
                                navController.pop()
                            }
                        },
                        showBottomItems = true,
                        topTitle = context.getString(R.string.sort),
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
                is Route.Config -> {
                    val tunnelName = sharedState.tunnelNames[route.id]
                    NavbarState(
                        topLeading = {
                            ActionIconButton(Icons.AutoMirrored.Rounded.ArrowBack, R.string.back) {
                                navController.pop()
                            }
                        },
                        showBottomItems = true,
                        topTitle = tunnelName ?: context.getString(R.string.new_tunnel),
                        topTrailing = {
                            ActionIconButton(Icons.Rounded.Save, R.string.save) {
                                keyboardController?.hide()
                                sharedViewModel.postSideEffect(LocalSideEffect.SaveChanges)
                            }
                        },
                    )
                }
                is Route.SplitTunnel -> {
                    val tunnelName = sharedState.tunnelNames[route.id]
                    NavbarState(
                        topLeading = {
                            ActionIconButton(Icons.AutoMirrored.Rounded.ArrowBack, R.string.back) {
                                navController.pop()
                            }
                        },
                        topTitle = tunnelName ?: "",
                        topTrailing = {
                            ActionIconButton(Icons.Rounded.Save, R.string.save) {
                                sharedViewModel.postSideEffect(LocalSideEffect.SaveChanges)
                            }
                        },
                        showBottomItems = true,
                    )
                }
                is Route.SplitTunnelGlobal -> {
                    NavbarState(
                        topLeading = {
                            ActionIconButton(Icons.AutoMirrored.Rounded.ArrowBack, R.string.back) {
                                navController.pop()
                            }
                        },
                        topTitle = context.getString(R.string.splt_tunneling),
                        topTrailing = {
                            ActionIconButton(Icons.Rounded.Save, R.string.save) {
                                sharedViewModel.postSideEffect(LocalSideEffect.SaveChanges)
                            }
                        },
                        showBottomItems = true,
                    )
                }
                is Route.ConfigGlobal -> {
                    NavbarState(
                        topLeading = {
                            ActionIconButton(Icons.AutoMirrored.Rounded.ArrowBack, R.string.back) {
                                navController.pop()
                            }
                        },
                        showBottomItems = true,
                        topTitle = context.getString(R.string.configuration),
                        topTrailing = {
                            ActionIconButton(Icons.Rounded.Save, R.string.save) {
                                keyboardController?.hide()
                                sharedViewModel.postSideEffect(LocalSideEffect.SaveChanges)
                            }
                        },
                    )
                }
                Route.Support ->
                    NavbarState(
                        topTitle = context.getString(R.string.support),
                        showBottomItems = true,
                    )
                Route.SystemFeatures ->
                    NavbarState(
                        topLeading = {
                            ActionIconButton(Icons.AutoMirrored.Rounded.ArrowBack, R.string.back) {
                                navController.pop()
                            }
                        },
                        topTitle = context.getString(R.string.android_integrations),
                        showBottomItems = true,
                    )
                is Route.TunnelAutoTunnel -> {
                    val tunnelName = sharedState.tunnelNames[route.id]
                    NavbarState(
                        topLeading = {
                            ActionIconButton(Icons.AutoMirrored.Rounded.ArrowBack, R.string.back) {
                                navController.pop()
                            }
                        },
                        showBottomItems = true,
                        topTitle = tunnelName ?: "",
                    )
                }
                Route.TunnelMonitoring ->
                    NavbarState(
                        topLeading = {
                            ActionIconButton(Icons.AutoMirrored.Rounded.ArrowBack, R.string.back) {
                                navController.pop()
                            }
                        },
                        topTitle = context.getString(R.string.tunnel_monitoring),
                        showBottomItems = true,
                    )
                is Route.TunnelOptions -> {
                    val tunnelName = sharedState.tunnelNames[route.id]
                    NavbarState(
                        topLeading = {
                            ActionIconButton(Icons.AutoMirrored.Rounded.ArrowBack, R.string.back) {
                                navController.pop()
                            }
                        },
                        showBottomItems = true,
                        topTitle = tunnelName ?: "",
                        topTrailing = {
                            Row {
                                ActionIconButton(Icons.Rounded.QrCode2, R.string.show_qr) {
                                    sharedViewModel.postSideEffect(LocalSideEffect.Modal.QR)
                                }
                                ActionIconButton(Icons.Rounded.Edit, R.string.edit_tunnel) {
                                    navController.push(Route.Config(route.id))
                                }
                            }
                        },
                    )
                }
                Route.Tunnels -> {
                    NavbarState(
                        topTitle = context.getString(R.string.tunnels),
                        topTrailing = {
                            when (sharedState.selectedTunnelCount) {
                                0 ->
                                    Row {
                                        ActionIconButton(
                                            Icons.AutoMirrored.Rounded.Sort,
                                            R.string.sort,
                                        ) {
                                            navController.push(Route.Sort)
                                        }
                                        ActionIconButton(Icons.Rounded.Add, R.string.add_tunnel) {
                                            sharedViewModel.postSideEffect(
                                                LocalSideEffect.Sheet.ImportTunnels
                                            )
                                        }
                                    }
                                else ->
                                    Row {
                                        ActionIconButton(
                                            Icons.Rounded.SelectAll,
                                            R.string.select_all,
                                        ) {
                                            sharedViewModel.postSideEffect(
                                                LocalSideEffect.SelectedTunnels.SelectAll
                                            )
                                        }
                                        // due to permissions, and SAF issues on TV, not support
                                        // less than Android
                                        // 10
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            ActionIconButton(
                                                Icons.Rounded.Download,
                                                R.string.download,
                                            ) {
                                                sharedViewModel.postSideEffect(
                                                    LocalSideEffect.Sheet.ExportTunnels
                                                )
                                            }
                                        }

                                        if (sharedState.selectedTunnelCount == 1) {
                                            ActionIconButton(Icons.Rounded.CopyAll, R.string.copy) {
                                                sharedViewModel.postSideEffect(
                                                    LocalSideEffect.SelectedTunnels.Copy
                                                )
                                            }
                                        }
                                        ActionIconButton(
                                            Icons.Rounded.Delete,
                                            R.string.delete_tunnel,
                                        ) {
                                            sharedViewModel.postSideEffect(
                                                LocalSideEffect.Modal.DeleteTunnels
                                            )
                                        }
                                    }
                            }
                        },
                        showBottomItems = true,
                    )
                }
                Route.WifiDetectionMethod ->
                    NavbarState(
                        topLeading = {
                            ActionIconButton(Icons.AutoMirrored.Rounded.ArrowBack, R.string.back) {
                                navController.pop()
                            }
                        },
                        topTitle = context.getString(R.string.wifi_detection_method),
                        showBottomItems = true,
                    )
                Route.Donate -> {
                    NavbarState(
                        topLeading = {
                            ActionIconButton(Icons.AutoMirrored.Rounded.ArrowBack, R.string.back) {
                                navController.pop()
                            }
                        },
                        topTitle = context.getString(R.string.donate_title),
                        showBottomItems = true,
                    )
                }
                Route.Addresses -> {
                    NavbarState(
                        topLeading = {
                            ActionIconButton(Icons.AutoMirrored.Rounded.ArrowBack, R.string.back) {
                                navController.pop()
                            }
                        },
                        topTitle = context.getString(R.string.addresses),
                        showBottomItems = true,
                    )
                }
                is Route.TunnelGlobals -> {
                    NavbarState(
                        topLeading = {
                            ActionIconButton(Icons.AutoMirrored.Rounded.ArrowBack, R.string.back) {
                                navController.pop()
                            }
                        },
                        topTitle = context.getString(R.string.tunnel_global_overrides),
                        showBottomItems = true,
                    )
                }
                null -> NavbarState()
            }
        }
    }
}
