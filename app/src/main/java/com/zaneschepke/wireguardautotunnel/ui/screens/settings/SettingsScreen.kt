package com.zaneschepke.wireguardautotunnel.ui.screens.settings

import android.Manifest
import android.app.Activity
import android.content.Context.POWER_SERVICE
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelState
import com.zaneschepke.wireguardautotunnel.ui.AppUiState
import com.zaneschepke.wireguardautotunnel.ui.AppViewModel
import com.zaneschepke.wireguardautotunnel.ui.Screens
import com.zaneschepke.wireguardautotunnel.ui.common.ClickableIconButton
import com.zaneschepke.wireguardautotunnel.ui.common.config.ConfigurationToggle
import com.zaneschepke.wireguardautotunnel.ui.common.prompt.AuthorizationPrompt
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarController
import com.zaneschepke.wireguardautotunnel.ui.common.text.SectionTitle
import com.zaneschepke.wireguardautotunnel.ui.screens.main.components.VpnDeniedDialog
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.BackgroundLocationDialog
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.BackgroundLocationDisclosure
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.LocationServicesDialog
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.WildcardSupportingLabel
import com.zaneschepke.wireguardautotunnel.util.extensions.isRunningOnTv
import com.zaneschepke.wireguardautotunnel.util.extensions.launchAppSettings
import com.zaneschepke.wireguardautotunnel.util.extensions.openWebUrl
import com.zaneschepke.wireguardautotunnel.util.extensions.showToast
import xyz.teamgravity.pin_lock_compose.PinManager

@OptIn(
	ExperimentalPermissionsApi::class,
	ExperimentalLayoutApi::class,
)
@Composable
fun SettingsScreen(
	viewModel: SettingsViewModel = hiltViewModel(),
	appViewModel: AppViewModel,
	uiState: AppUiState,
	navController: NavController,
	focusRequester: FocusRequester,
) {
	val context = LocalContext.current
	val focusManager = LocalFocusManager.current
	val snackbar = SnackbarController.current
	val scrollState = rememberScrollState()
	val interactionSource = remember { MutableInteractionSource() }
	val isRunningOnTv = context.isRunningOnTv()

	val kernelSupport by viewModel.kernelSupport.collectAsStateWithLifecycle()

	val fineLocationState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
	var currentText by remember { mutableStateOf("") }
	var isBackgroundLocationGranted by remember { mutableStateOf(true) }
	var showVpnPermissionDialog by remember { mutableStateOf(false) }
	var showLocationServicesAlertDialog by remember { mutableStateOf(false) }
	val didExportFiles by remember { mutableStateOf(false) }
	var showAuthPrompt by remember { mutableStateOf(false) }
	var showLocationDialog by remember { mutableStateOf(false) }

	val screenPadding = 5.dp
	val fillMaxWidth = .85f

	LaunchedEffect(Unit) {
		viewModel.checkKernelSupport()
	}

	LaunchedEffect(uiState.settings.trustedNetworkSSIDs) {
		currentText = ""
	}

	val startForResult =
		rememberLauncherForActivityResult(
			ActivityResultContracts.StartActivityForResult(),
		) { result: ActivityResult ->
			if (result.resultCode == Activity.RESULT_OK) {
				result.data
				// Handle the Intent
			}
			viewModel.setBatteryOptimizeDisableShown()
		}

	val vpnActivityResultState =
		rememberLauncherForActivityResult(
			ActivityResultContracts.StartActivityForResult(),
			onResult = {
				val accepted = (it.resultCode == RESULT_OK)
				if (accepted) {
					viewModel.onToggleAutoTunnel(context)
				} else {
					showVpnPermissionDialog = true
				}
			},
		)

	fun isBatteryOptimizationsDisabled(): Boolean {
		val pm = context.getSystemService(POWER_SERVICE) as PowerManager
		return pm.isIgnoringBatteryOptimizations(context.packageName)
	}

	fun requestBatteryOptimizationsDisabled() {
		val intent =
			Intent().apply {
				action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
				data = Uri.parse("package:${context.packageName}")
			}
		startForResult.launch(intent)
	}

	fun handleAutoTunnelToggle() {
		if (!uiState.generalState.isBatteryOptimizationDisableShown &&
			!isBatteryOptimizationsDisabled() && !context.isRunningOnTv()
		) {
			return requestBatteryOptimizationsDisabled()
		}
		val intent = if (!uiState.settings.isKernelEnabled) {
			VpnService.prepare(context)
		} else {
			null
		}
		if (intent != null) return vpnActivityResultState.launch(intent)
		viewModel.onToggleAutoTunnel(context)
	}

	fun saveTrustedSSID() {
		if (currentText.isNotEmpty()) {
			viewModel.onSaveTrustedSSID(currentText)
		}
	}

	fun checkFineLocationGranted() {
		isBackgroundLocationGranted =
			if (!fineLocationState.status.isGranted) {
				false
			} else {
				viewModel.setLocationDisclosureShown()
				true
			}
	}

	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
		if (
			isRunningOnTv &&
			Build.VERSION.SDK_INT == Build.VERSION_CODES.Q
		) {
			checkFineLocationGranted()
		} else {
			val backgroundLocationState =
				rememberPermissionState(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
			isBackgroundLocationGranted =
				if (!backgroundLocationState.status.isGranted) {
					false
				} else {
					SideEffect { viewModel.setLocationDisclosureShown() }
					true
				}
		}
	}

	if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
		checkFineLocationGranted()
	}
	if (!uiState.generalState.isLocationDisclosureShown) {
		BackgroundLocationDisclosure(
			onDismiss = { viewModel.setLocationDisclosureShown() },
			onAttest = {
				context.launchAppSettings()
				viewModel.setLocationDisclosureShown()
			},
			scrollState,
			focusRequester,
		)
		return
	}

	BackgroundLocationDialog(
		showLocationDialog,
		onDismiss = { showLocationDialog = false },
		onAttest = { showLocationDialog = false },
	)

	LocationServicesDialog(
		showLocationServicesAlertDialog,
		onDismiss = { showVpnPermissionDialog = false },
		onAttest = { handleAutoTunnelToggle() },
	)

	VpnDeniedDialog(showVpnPermissionDialog, onDismiss = { showVpnPermissionDialog = false })

	if (showAuthPrompt) {
		AuthorizationPrompt(
			onSuccess = {
				showAuthPrompt = false
				viewModel.exportAllConfigs()
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
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Top,
		modifier =
		Modifier
			.fillMaxSize()
			.verticalScroll(scrollState)
			.clickable(
				indication = null,
				interactionSource = interactionSource,
			) {
				focusManager.clearFocus()
			},
	) {
		Surface(
			tonalElevation = 2.dp,
			shadowElevation = 2.dp,
			shape = RoundedCornerShape(12.dp),
			color = MaterialTheme.colorScheme.surface,
			modifier =
			(
				if (isRunningOnTv) {
					Modifier
						.height(IntrinsicSize.Min)
						.fillMaxWidth(fillMaxWidth)
						.padding(top = 10.dp)
				} else {
					Modifier
						.fillMaxWidth(fillMaxWidth)
						.padding(top = 20.dp)
				}
				)
				.padding(bottom = 10.dp),
		) {
			Column(
				horizontalAlignment = Alignment.Start,
				verticalArrangement = Arrangement.Top,
				modifier = Modifier.padding(15.dp),
			) {
				SectionTitle(
					title = stringResource(id = R.string.auto_tunneling),
					padding = screenPadding,
				)
				ConfigurationToggle(
					stringResource(id = R.string.tunnel_on_wifi),
					enabled = !uiState.settings.isAlwaysOnVpnEnabled,
					checked = uiState.settings.isTunnelOnWifiEnabled,
					padding = screenPadding,
					onCheckChanged = { checked ->
						if (!checked) viewModel.onToggleTunnelOnWifi()
						if (checked) {
							when (false) {
								isBackgroundLocationGranted -> showLocationDialog = true
								fineLocationState.status.isGranted -> showLocationDialog = true
								viewModel.isLocationEnabled(context) ->
									showLocationServicesAlertDialog = true
								else -> {
									viewModel.onToggleTunnelOnWifi()
								}
							}
						}
					},
					modifier =
					if (uiState.settings.isAutoTunnelEnabled) {
						Modifier
					} else {
						Modifier
							.focusRequester(focusRequester)
					},
				)
				if (uiState.settings.isTunnelOnWifiEnabled) {
					Column {
						FlowRow(
							modifier =
							Modifier
								.padding(screenPadding)
								.fillMaxWidth(),
							horizontalArrangement = Arrangement.spacedBy(5.dp),
						) {
							uiState.settings.trustedNetworkSSIDs.forEach { ssid ->
								ClickableIconButton(
									onClick = {
										if (isRunningOnTv) {
											focusRequester.requestFocus()
											viewModel.onDeleteTrustedSSID(ssid)
										}
									},
									onIconClick = {
										if (isRunningOnTv) focusRequester.requestFocus()
										viewModel.onDeleteTrustedSSID(ssid)
									},
									text = ssid,
									icon = Icons.Filled.Close,
								)
							}
							if (uiState.settings.trustedNetworkSSIDs.isEmpty()) {
								Text(
									stringResource(R.string.none),
									fontStyle = FontStyle.Italic,
									style = MaterialTheme.typography.bodySmall,
									color = MaterialTheme.colorScheme.onSurface,
								)
							}
						}
						OutlinedTextField(
							value = currentText,
							onValueChange = { currentText = it },
							label = { Text(stringResource(R.string.add_trusted_ssid)) },
							modifier =
							Modifier
								.padding(
									start = screenPadding,
									top = 5.dp,
									bottom = 10.dp,
								),
							supportingText = { WildcardSupportingLabel { context.openWebUrl(it) } },
							maxLines = 1,
							keyboardOptions =
							KeyboardOptions(
								capitalization = KeyboardCapitalization.None,
								imeAction = ImeAction.Done,
							),
							keyboardActions = KeyboardActions(onDone = { saveTrustedSSID() }),
							trailingIcon = {
								if (currentText != "") {
									IconButton(onClick = { saveTrustedSSID() }) {
										Icon(
											imageVector = Icons.Outlined.Add,
											contentDescription =
											if (currentText == "") {
												stringResource(
													id =
													R.string
														.trusted_ssid_empty_description,
												)
											} else {
												stringResource(
													id =
													R.string
														.trusted_ssid_value_description,
												)
											},
											tint = MaterialTheme.colorScheme.primary,
										)
									}
								}
							},
						)
					}
				}
				ConfigurationToggle(
					stringResource(R.string.tunnel_mobile_data),
					enabled = !uiState.settings.isAlwaysOnVpnEnabled,
					checked = uiState.settings.isTunnelOnMobileDataEnabled,
					padding = screenPadding,
					onCheckChanged = { viewModel.onToggleTunnelOnMobileData() },
				)
				ConfigurationToggle(
					stringResource(id = R.string.tunnel_on_ethernet),
					enabled = !uiState.settings.isAlwaysOnVpnEnabled,
					checked = uiState.settings.isTunnelOnEthernetEnabled,
					padding = screenPadding,
					onCheckChanged = { viewModel.onToggleTunnelOnEthernet() },
				)
				ConfigurationToggle(
					stringResource(R.string.restart_on_ping),
					checked = uiState.settings.isPingEnabled,
					padding = screenPadding,
					onCheckChanged = { viewModel.onToggleRestartOnPing() },
				)
				Row(
					verticalAlignment = Alignment.CenterVertically,
					modifier =
					(
						if (!uiState.settings.isAutoTunnelEnabled) {
							Modifier
						} else {
							Modifier.focusRequester(
								focusRequester,
							)
						}
						)
						.fillMaxSize()
						.padding(top = 5.dp),
					horizontalArrangement = Arrangement.Center,
				) {
					TextButton(
						onClick = {
							if (uiState.tunnels.isEmpty()) return@TextButton context.showToast(R.string.tunnel_required)
							handleAutoTunnelToggle()
						},
					) {
						val autoTunnelButtonText =
							if (uiState.settings.isAutoTunnelEnabled) {
								stringResource(R.string.disable_auto_tunnel)
							} else {
								stringResource(id = R.string.enable_auto_tunnel)
							}
						Text(autoTunnelButtonText)
					}
				}
			}
		}
		Surface(
			tonalElevation = 2.dp,
			shadowElevation = 2.dp,
			shape = RoundedCornerShape(12.dp),
			color = MaterialTheme.colorScheme.surface,
			modifier =
			Modifier
				.fillMaxWidth(fillMaxWidth)
				.padding(vertical = 10.dp),
		) {
			Column(
				horizontalAlignment = Alignment.Start,
				verticalArrangement = Arrangement.Top,
				modifier = Modifier.padding(15.dp),
			) {
				SectionTitle(
					title = stringResource(id = R.string.backend),
					padding = screenPadding,
				)
				ConfigurationToggle(
					stringResource(R.string.use_kernel),
					enabled =
					!(
						uiState.settings.isAutoTunnelEnabled ||
							uiState.settings.isAlwaysOnVpnEnabled ||
							(uiState.vpnState.status == TunnelState.UP) ||
							!kernelSupport
						),
					checked = uiState.settings.isKernelEnabled,
					padding = screenPadding,
					onCheckChanged = {
						viewModel.onToggleKernelMode()
					},
				)
				Row(
					verticalAlignment = Alignment.CenterVertically,
					modifier =
					Modifier
						.fillMaxSize()
						.padding(top = 5.dp),
					horizontalArrangement = Arrangement.Center,
				) {
					TextButton(
						onClick = {
							viewModel.onRequestRoot()
						},
					) {
						Text(stringResource(R.string.request_root))
					}
				}
			}
		}
		Surface(
			tonalElevation = 2.dp,
			shadowElevation = 2.dp,
			shape = RoundedCornerShape(12.dp),
			color = MaterialTheme.colorScheme.surface,
			modifier =
			Modifier
				.fillMaxWidth(fillMaxWidth)
				.padding(vertical = 10.dp)
				.padding(bottom = 10.dp),
		) {
			Column(
				horizontalAlignment = Alignment.Start,
				verticalArrangement = Arrangement.Top,
				modifier = Modifier.padding(15.dp),
			) {
				SectionTitle(
					title = stringResource(id = R.string.other),
					padding = screenPadding,
				)
				if (!isRunningOnTv) {
					ConfigurationToggle(
						stringResource(R.string.always_on_vpn_support),
						enabled = !(
							uiState.settings.isTunnelOnWifiEnabled ||
								uiState.settings.isTunnelOnWifiEnabled ||
								uiState.settings.isTunnelOnMobileDataEnabled
							),
						checked = uiState.settings.isAlwaysOnVpnEnabled,
						padding = screenPadding,
						onCheckChanged = { viewModel.onToggleAlwaysOnVPN() },
					)
					ConfigurationToggle(
						stringResource(R.string.enabled_app_shortcuts),
						enabled = true,
						checked = uiState.settings.isShortcutsEnabled,
						padding = screenPadding,
						onCheckChanged = { viewModel.onToggleShortcutsEnabled() },
					)
				}
				ConfigurationToggle(
					stringResource(R.string.restart_at_boot),
					enabled = true,
					checked = uiState.settings.isRestoreOnBootEnabled,
					padding = screenPadding,
					onCheckChanged = {
						viewModel.onToggleRestartAtBoot()
					},
				)
				ConfigurationToggle(
					stringResource(R.string.enable_app_lock),
					enabled = true,
					checked = uiState.generalState.isPinLockEnabled,
					padding = screenPadding,
					onCheckChanged = {
						if (uiState.generalState.isPinLockEnabled) {
							appViewModel.onPinLockDisabled()
						} else {
							// TODO may want to show a dialog before proceeding in the future
							PinManager.initialize(WireGuardAutoTunnel.instance)
							navController.navigate(Screens.Lock)
						}
					},
				)
				if (!isRunningOnTv) {
					Row(
						verticalAlignment = Alignment.CenterVertically,
						modifier =
						Modifier
							.fillMaxSize()
							.padding(top = 5.dp),
						horizontalArrangement = Arrangement.Center,
					) {
						TextButton(
							enabled = !didExportFiles,
							onClick = {
								if (uiState.tunnels.isEmpty()) return@TextButton context.showToast(R.string.tunnel_required)
								showAuthPrompt = true
							},
						) {
							Text(stringResource(R.string.export_configs))
						}
					}
				}
			}
		}
	}
}
