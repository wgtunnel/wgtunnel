package com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.SectionDivider
import com.zaneschepke.wireguardautotunnel.ui.common.banner.WarningBanner
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.common.dialog.InfoDialog
import com.zaneschepke.wireguardautotunnel.ui.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.components.AdvancedSettingsItem
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.components.NetworkTunnelingItems
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.components.WifiTunnelingItems
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.util.extensions.launchAppSettings
import com.zaneschepke.wireguardautotunnel.util.extensions.launchLocationServicesSettings
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AutoTunnelScreen(uiState: AppUiState, viewModel: AppViewModel) {
    val navController = LocalNavController.current
    val context = LocalContext.current

    var currentText by remember { mutableStateOf("") }
    var showLocationDialog by remember { mutableStateOf(false) }

    val showLocationServicesWarning by
        remember(
            uiState.connectivityState?.wifiState,
            uiState.appSettings.trustedNetworkSSIDs,
            uiState.appSettings.wifiDetectionMethod,
        ) {
            derivedStateOf {
                uiState.connectivityState?.wifiState?.locationServicesEnabled == false &&
                    uiState.appSettings.wifiDetectionMethod.needsLocationPermissions() &&
                    uiState.appSettings.trustedNetworkSSIDs.isNotEmpty()
            }
        }

    val showLocationPermissionsWarning by
        remember(
            uiState.connectivityState?.wifiState,
            uiState.appSettings.trustedNetworkSSIDs,
            uiState.appSettings.wifiDetectionMethod,
        ) {
            derivedStateOf {
                uiState.connectivityState?.wifiState?.locationPermissionsGranted == false &&
                    uiState.appSettings.wifiDetectionMethod.needsLocationPermissions() &&
                    uiState.appSettings.trustedNetworkSSIDs.isNotEmpty()
            }
        }

    LaunchedEffect(uiState.appSettings.trustedNetworkSSIDs) { currentText = "" }

    if (showLocationDialog) {
        InfoDialog(
            onAttest = {
                context.launchAppSettings()
                showLocationDialog = false
            },
            onDismiss = { showLocationDialog = false },
            title = { Text(stringResource(R.string.location_permissions)) },
            body = { Text(stringResource(R.string.location_justification)) },
            confirmText = { Text(stringResource(R.string.open_settings)) },
        )
    }

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
        modifier =
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp)
                .padding(horizontal = 12.dp),
    ) {
        WarningBanner(
            stringResource(R.string.location_services_not_detected),
            showLocationServicesWarning,
            trailing = {
                TextButton({ context.launchLocationServicesSettings() }) {
                    Text(
                        stringResource(R.string.fix),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
        )
        WarningBanner(
            stringResource(R.string.location_permissions_missing),
            showLocationPermissionsWarning,
            trailing = {
                TextButton({ showLocationDialog = true }) {
                    Text(
                        stringResource(R.string.fix),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
        )
        val (title, buttonText, icon) =
            remember(uiState.isAutoTunnelActive) {
                when (uiState.isAutoTunnelActive) {
                    true ->
                        Triple(
                            context.getString(R.string.auto_tunnel_running),
                            context.getString(R.string.stop),
                            Icons.Outlined.CheckCircle,
                        )
                    false ->
                        Triple(
                            context.getString(R.string.auto_tunnel_not_running),
                            context.getString(R.string.start),
                            Icons.Outlined.Info,
                        )
                }
            }
        SurfaceSelectionGroupButton(
            items =
                listOf(
                    SelectionItem(
                        leading = { Icon(icon, null) },
                        title = { Text(title) },
                        trailing = {
                            Button({ viewModel.handleEvent(AppEvent.ToggleAutoTunnel) }) {
                                Text(buttonText, fontWeight = FontWeight.Bold)
                            }
                        },
                    )
                )
        )
        SurfaceSelectionGroupButton(
            items = WifiTunnelingItems(uiState, viewModel, currentText) { currentText = it }
        )
        SectionDivider()
        SurfaceSelectionGroupButton(items = NetworkTunnelingItems(uiState, viewModel))
        SectionDivider()
        SurfaceSelectionGroupButton(
            items =
                listOf(
                    AdvancedSettingsItem(
                        onClick = { navController.navigate(Route.AutoTunnelAdvanced) }
                    )
                )
        )
    }
}
