package com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.wifi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Filter1
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.WifiFind
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.banner.WarningBanner
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.ui.common.dialog.InfoDialog
import com.zaneschepke.wireguardautotunnel.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.ui.common.text.DescriptionText
import com.zaneschepke.wireguardautotunnel.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.ui.navigation.TunnelNetwork
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.components.TrustedNetworkTextBox
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.components.WildcardsLabel
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.LearnMoreLinkLabel
import com.zaneschepke.wireguardautotunnel.util.extensions.asTitleString
import com.zaneschepke.wireguardautotunnel.util.extensions.launchAppSettings
import com.zaneschepke.wireguardautotunnel.util.extensions.launchLocationServicesSettings
import com.zaneschepke.wireguardautotunnel.util.extensions.openWebUrl
import com.zaneschepke.wireguardautotunnel.viewmodel.AutoTunnelViewModel

@Composable
fun WifiSettingsScreen(viewModel: AutoTunnelViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val navController = LocalNavController.current

    val autoTunnelState by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    if (autoTunnelState.isLoading) return

    var showLocationDialog by remember { mutableStateOf(false) }
    var currentText by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(autoTunnelState.autoTunnelSettings.trustedNetworkSSIDs) { currentText = "" }

    val warnings by
        remember(
            autoTunnelState.connectivityState?.wifiState,
            autoTunnelState.autoTunnelSettings.trustedNetworkSSIDs,
            autoTunnelState.autoTunnelSettings.wifiDetectionMethod,
            autoTunnelState.tunnels,
        ) {
            derivedStateOf {
                val wifiState = autoTunnelState.connectivityState?.wifiState
                val needsLocation =
                    autoTunnelState.autoTunnelSettings.wifiDetectionMethod
                        .needsLocationPermissions()
                val hasConfigs =
                    autoTunnelState.autoTunnelSettings.trustedNetworkSSIDs.isNotEmpty() ||
                        autoTunnelState.tunnels.any { it.tunnelNetworks.isNotEmpty() }

                val showServicesWarning =
                    (wifiState?.locationServicesEnabled == false) && needsLocation && hasConfigs
                val showPermissionsWarning =
                    (wifiState?.locationPermissionsGranted == false) && needsLocation && hasConfigs

                showServicesWarning to showPermissionsWarning
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
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        Column {
            WarningBanner(
                stringResource(R.string.location_services_not_detected),
                warnings.first,
                modifier = Modifier.padding(horizontal = 16.dp),
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
                warnings.second,
                modifier = Modifier.padding(horizontal = 16.dp),
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
        }
        Column {
            GroupLabel(stringResource(R.string.general), Modifier.padding(horizontal = 16.dp))
            SurfaceRow(
                leading = { Icon(Icons.Outlined.WifiFind, contentDescription = null) },
                title = stringResource(R.string.wifi_detection_method),
                description = {
                    DescriptionText(
                        stringResource(
                            R.string.current_template,
                            autoTunnelState.autoTunnelSettings.wifiDetectionMethod.asTitleString(
                                context
                            ),
                        )
                    )
                },
                onClick = { navController.push(Route.WifiDetectionMethod) },
            )
            SurfaceRow(
                leading = { Icon(Icons.Outlined.Filter1, contentDescription = null) },
                title = stringResource(R.string.use_wildcards),
                description = {
                    LearnMoreLinkLabel(
                        { context.openWebUrl(it) },
                        stringResource(R.string.docs_wildcards),
                    )
                },
                trailing = {
                    ScaledSwitch(
                        checked = autoTunnelState.autoTunnelSettings.isWildcardsEnabled,
                        onClick = { viewModel.setWildcardsEnabled(it) },
                    )
                },
                onClick = {
                    viewModel.setWildcardsEnabled(
                        !autoTunnelState.autoTunnelSettings.isWildcardsEnabled
                    )
                },
            )
        }
        Column {
            GroupLabel(stringResource(R.string.networks), Modifier.padding(horizontal = 16.dp))
            SurfaceRow(
                title = stringResource(R.string.trusted_wifi_names),
                expandedContent = {
                    TrustedNetworkTextBox(
                        autoTunnelState.autoTunnelSettings.trustedNetworkSSIDs,
                        onDelete = { viewModel.removeTrustedNetworkName(it) },
                        currentText = currentText,
                        onSave = { ssid -> viewModel.saveTrustedNetworkName(ssid) },
                        onValueChange = { currentText = it },
                        supporting = {
                            if (autoTunnelState.autoTunnelSettings.isWildcardsEnabled)
                                WildcardsLabel()
                        },
                        modifier = Modifier.padding(top = 4.dp),
                    )
                },
            )
        }
        Column {
            GroupLabel(stringResource(R.string.tunnels), Modifier.padding(horizontal = 16.dp))
            SurfaceRow(
                leading = { Icon(Icons.Outlined.Map, contentDescription = null) },
                title = stringResource(R.string.tunnel_mapping),
                onClick = { navController.push(Route.PreferredTunnel(TunnelNetwork.WIFI)) },
            )
        }
    }
}
