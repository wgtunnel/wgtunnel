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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.SectionDivider
import com.zaneschepke.wireguardautotunnel.ui.common.banner.WarningBanner
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.common.dialog.InfoDialog
import com.zaneschepke.wireguardautotunnel.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.components.AdvancedSettingsItem
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.components.networkTunnelingItems
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.components.wifiTunnelingItems
import com.zaneschepke.wireguardautotunnel.util.extensions.launchAppSettings
import com.zaneschepke.wireguardautotunnel.util.extensions.launchLocationServicesSettings
import com.zaneschepke.wireguardautotunnel.viewmodel.AutoTunnelViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AutoTunnelScreen(viewModel: AutoTunnelViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val autoTunnelState by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(autoTunnelState.stateInitialized) {
        if (!autoTunnelState.isLocationDisclosureShown && autoTunnelState.stateInitialized) {
            navController.push(Route.LocationDisclosure)
        }
    }

    if (!autoTunnelState.stateInitialized) return
    var showLocationDialog by remember { mutableStateOf(false) }

    val showLocationServicesWarning by
        remember(
            autoTunnelState.connectivityState?.wifiState,
            autoTunnelState.settings.trustedNetworkSSIDs,
            autoTunnelState.settings.wifiDetectionMethod,
        ) {
            derivedStateOf {
                autoTunnelState.connectivityState?.wifiState?.locationServicesEnabled == false &&
                    autoTunnelState.settings.wifiDetectionMethod.needsLocationPermissions() &&
                    autoTunnelState.settings.trustedNetworkSSIDs.isNotEmpty()
            }
        }

    val showLocationPermissionsWarning by
        remember(
            autoTunnelState.connectivityState?.wifiState,
            autoTunnelState.settings.trustedNetworkSSIDs,
            autoTunnelState.settings.wifiDetectionMethod,
        ) {
            derivedStateOf {
                autoTunnelState.connectivityState?.wifiState?.locationPermissionsGranted == false &&
                    autoTunnelState.settings.wifiDetectionMethod.needsLocationPermissions() &&
                    autoTunnelState.settings.trustedNetworkSSIDs.isNotEmpty()
            }
        }

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
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
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
            remember(autoTunnelState.autoTunnelActive) {
                when (autoTunnelState.autoTunnelActive) {
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
                            Button({ viewModel.toggleAutoTunnel() }) {
                                Text(
                                    buttonText,
                                    fontWeight = FontWeight.Bold,
                                    style =
                                        MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.surface
                                        ),
                                )
                            }
                        },
                    )
                )
        )
        SurfaceSelectionGroupButton(items = wifiTunnelingItems(autoTunnelState, viewModel))
        SectionDivider()
        SurfaceSelectionGroupButton(items = networkTunnelingItems(autoTunnelState, viewModel))
        SectionDivider()
        SurfaceSelectionGroupButton(
            items =
                listOf(
                    AdvancedSettingsItem(onClick = { navController.push(Route.AdvancedAutoTunnel) })
                )
        )
    }
}
