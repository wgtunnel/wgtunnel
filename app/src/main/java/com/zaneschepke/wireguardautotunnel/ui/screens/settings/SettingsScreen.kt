package com.zaneschepke.wireguardautotunnel.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewQuilt
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.VpnKeyOff
import androidx.compose.material.icons.outlined.VpnLock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.prompt.AuthorizationPrompt
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarController
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.ForwardButton
import com.zaneschepke.wireguardautotunnel.util.extensions.isRunningOnTv
import com.zaneschepke.wireguardautotunnel.util.extensions.launchNotificationSettings
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledHeight
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledWidth
import com.zaneschepke.wireguardautotunnel.util.extensions.showToast
import com.zaneschepke.wireguardautotunnel.viewmodel.SettingsViewModel
import xyz.teamgravity.pin_lock_compose.PinManager

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel(), appViewModel: AppViewModel, uiState: AppUiState) {
	val context = LocalContext.current
	val navController = LocalNavController.current
	val focusManager = LocalFocusManager.current
	val snackbar = SnackbarController.current
	val isRunningOnTv = remember { context.isRunningOnTv() }

	val interactionSource = remember { MutableInteractionSource() }
	var showAuthPrompt by remember { mutableStateOf(false) }

	if (showAuthPrompt) {
		AuthorizationPrompt(
			onSuccess = {
				showAuthPrompt = false
				viewModel.exportAllConfigs(context)
			},
			onError = { _ ->
				showAuthPrompt = false
				snackbar.showMessage(
					context.getString(R.string.error_authentication_failed),
				)
			},
			onFailure = {
				showAuthPrompt = false
				snackbar.showMessage(
					context.getString(R.string.error_authorization_failed),
				)
			},
		)
	}

	Column(
		horizontalAlignment = Alignment.Start,
		verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
		modifier =
		Modifier
			.verticalScroll(rememberScrollState())
			.fillMaxSize().systemBarsPadding().imePadding()
			.padding(top = 24.dp.scaledHeight())
			.padding(bottom = 40.dp.scaledHeight())
			.padding(horizontal = 24.dp.scaledWidth())
			.then(
				if (!isRunningOnTv) {
					Modifier.clickable(
						indication = null,
						interactionSource = interactionSource,
					) {
						focusManager.clearFocus()
					}
				} else {
					Modifier
				},
			),
	) {
		val onAutoTunnelClick = {
			if (!uiState.generalState.isLocationDisclosureShown) {
				navController.navigate(Route.LocationDisclosure)
			} else {
				navController.navigate(Route.AutoTunnel)
			}
		}
		SurfaceSelectionGroupButton(
			listOf(
				SelectionItem(
					Icons.Outlined.Bolt,
					title = { Text(stringResource(R.string.auto_tunneling), style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface)) },
					description = {
						Text(
							stringResource(R.string.on_demand_rules),
							style = MaterialTheme.typography.bodySmall.copy(MaterialTheme.colorScheme.outline),
						)
					},
					onClick = {
						onAutoTunnelClick()
					},
					trailing = {
						ForwardButton(Modifier.focusable()) { onAutoTunnelClick() }
					},
				),
			),
		)
		SurfaceSelectionGroupButton(
			buildList {
				add(
					SelectionItem(
						Icons.Filled.AppShortcut,
						{
							ScaledSwitch(
								uiState.appSettings.isShortcutsEnabled,
								onClick = { appViewModel.onToggleShortcutsEnabled() },
							)
						},
						title = {
							Text(
								stringResource(R.string.enabled_app_shortcuts),
								style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
							)
						},
						onClick = { appViewModel.onToggleShortcutsEnabled() },
					),
				)
				if (!isRunningOnTv) {
					add(
						SelectionItem(
							Icons.Outlined.VpnLock,
							{
								ScaledSwitch(
									enabled = !(
										(
											uiState.appSettings.isTunnelOnWifiEnabled ||
												uiState.appSettings.isTunnelOnEthernetEnabled ||
												uiState.appSettings.isTunnelOnMobileDataEnabled
											) &&
											uiState.appSettings.isAutoTunnelEnabled
										),
									onClick = { appViewModel.onToggleAlwaysOnVPN() },
									checked = uiState.appSettings.isAlwaysOnVpnEnabled,
								)
							},
							title = {
								Text(
									stringResource(R.string.always_on_vpn_support),
									style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
								)
							},
							onClick = { appViewModel.onToggleAlwaysOnVPN() },
						),
					)
				}
				add(
					SelectionItem(
						Icons.Outlined.VpnKeyOff,
						title = {
							Text(
								stringResource(R.string.kill_switch_options),
								style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
							)
						},
						onClick = {
							navController.navigate(Route.KillSwitch)
						},
						trailing = {
							ForwardButton { navController.navigate(Route.KillSwitch) }
						},
					),
				)

				add(
					SelectionItem(
						Icons.Outlined.Restore,
						{
							ScaledSwitch(
								uiState.appSettings.isRestoreOnBootEnabled,
								onClick = { appViewModel.onToggleRestartAtBoot() },
							)
						},
						title = {
							Text(
								stringResource(R.string.restart_at_boot),
								style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
							)
						},
						onClick = { appViewModel.onToggleRestartAtBoot() },
					),
				)
			},
		)

		SurfaceSelectionGroupButton(
			listOf(
				SelectionItem(
					Icons.AutoMirrored.Outlined.ViewQuilt,
					title = { Text(stringResource(R.string.appearance), style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface)) },
					onClick = {
						navController.navigate(Route.Appearance)
					},
					trailing = {
						ForwardButton { navController.navigate(Route.Appearance) }
					},
				),
				SelectionItem(
					Icons.Outlined.Notifications,
					title = { Text(stringResource(R.string.notifications), style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface)) },
					onClick = {
						context.launchNotificationSettings()
					},
					trailing = {
						ForwardButton { context.launchNotificationSettings() }
					},
				),
				SelectionItem(
					Icons.Outlined.Pin,
					title = {
						Text(
							stringResource(R.string.enable_app_lock),
							style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
						)
					},
					trailing = {
						ScaledSwitch(
							uiState.generalState.isPinLockEnabled,
							onClick = {
								if (uiState.generalState.isPinLockEnabled) {
									appViewModel.onPinLockDisabled()
								} else {
									PinManager.initialize(context)
									navController.navigate(Route.Lock)
								}
							},
						)
					},
					onClick = {
						if (uiState.generalState.isPinLockEnabled) {
							appViewModel.onPinLockDisabled()
						} else {
							PinManager.initialize(context)
							navController.navigate(Route.Lock)
						}
					},
				),
			),
		)

		if (!isRunningOnTv) {
			SurfaceSelectionGroupButton(
				listOf(
					SelectionItem(
						Icons.Outlined.Code,
						title = { Text(stringResource(R.string.kernel), style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface)) },
						description = {
							Text(
								stringResource(R.string.use_kernel),
								style = MaterialTheme.typography.bodySmall.copy(MaterialTheme.colorScheme.outline),
							)
						},
						trailing = {
							ScaledSwitch(
								uiState.appSettings.isKernelEnabled,
								onClick = { appViewModel.onToggleKernelMode() },
// 								enabled = !(
// 									uiState.settings.isAutoTunnelEnabled ||
// 										uiState.settings.isAlwaysOnVpnEnabled ||
// 										(uiState.vpnState.status == TunnelState.UP)
// 									),
							)
						},
						onClick = {
							appViewModel.onToggleKernelMode()
						},
					),
				),
			)
		}

		if (!isRunningOnTv) {
			SurfaceSelectionGroupButton(
				listOf(
					SelectionItem(
						Icons.Outlined.FolderZip,
						title = {
							Text(
								stringResource(R.string.export_configs),
								style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
							)
						},
						onClick = {
							if (uiState.tunnels.isEmpty()) return@SelectionItem context.showToast(R.string.tunnel_required)
							showAuthPrompt = true
						},
					),
				),
			)
		}
	}
}
