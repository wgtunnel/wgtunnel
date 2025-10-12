package com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.banner.WarningBanner
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.ui.common.button.SwitchWithDivider
import com.zaneschepke.wireguardautotunnel.ui.common.dialog.InfoDialog
import com.zaneschepke.wireguardautotunnel.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.ui.navigation.TunnelNetwork
import com.zaneschepke.wireguardautotunnel.util.extensions.launchAppSettings
import com.zaneschepke.wireguardautotunnel.util.extensions.launchLocationServicesSettings
import com.zaneschepke.wireguardautotunnel.viewmodel.AutoTunnelViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AutoTunnelScreen(viewModel: AutoTunnelViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val autoTunnelState by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    if (autoTunnelState.isLoading) return
    var showLocationDialog by remember { mutableStateOf(false) }

    val (ethernetTunnel, mobileDataTunnel, mappedTunnels) =
        remember(autoTunnelState.tunnels) {
            Triple(
                autoTunnelState.tunnels.firstOrNull { it.isEthernetTunnel },
                autoTunnelState.tunnels.firstOrNull { it.isMobileDataTunnel },
                autoTunnelState.tunnels.any { it.tunnelNetworks.isNotEmpty() },
            )
        }

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
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        Column {
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
            SurfaceRow(
                leading = { Icon(icon, null) },
                title = title,
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
                onClick = { viewModel.toggleAutoTunnel() },
            )
        }
        Column {
            GroupLabel(
                stringResource(R.string.networks),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            SurfaceRow(
                leading = { Icon(Icons.Outlined.Wifi, contentDescription = null) },
                title = stringResource(R.string.tunnel_on_wifi),
                trailing = { modifier ->
                    SwitchWithDivider(
                        checked = autoTunnelState.settings.isTunnelOnWifiEnabled,
                        onClick = { viewModel.setAutoTunnelOnWifiEnabled(it) },
                        modifier = modifier,
                    )
                },
                description = {
                    Text(
                        buildAnnotatedString {
                            append(stringResource(R.string.preferred_tunnel_label))
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(
                                    if (mappedTunnels) stringResource(R.string.mapped)
                                    else stringResource(R.string._default)
                                )
                            }
                        },
                        style =
                            MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.outline
                            ),
                    )
                },
                onClick = { navController.push(Route.WifiPreferences) },
            )
            SurfaceRow(
                leading = { Icon(Icons.Outlined.SignalCellular4Bar, contentDescription = null) },
                title = stringResource(R.string.tunnel_mobile_data),
                trailing = { modifier ->
                    SwitchWithDivider(
                        checked = autoTunnelState.settings.isTunnelOnMobileDataEnabled,
                        onClick = { viewModel.setTunnelOnCellular(it) },
                        modifier = modifier,
                    )
                },
                description = {
                    Text(
                        buildAnnotatedString {
                            append(stringResource(R.string.preferred_tunnel_label))
                            mobileDataTunnel?.tunName?.let { append(it) }
                                ?: withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(stringResource(R.string._default))
                                }
                        },
                        style =
                            MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.outline
                            ),
                    )
                },
                onClick = { navController.push(Route.PreferredTunnel(TunnelNetwork.MOBILE_DATA)) },
            )
            SurfaceRow(
                leading = { Icon(Icons.Outlined.SettingsEthernet, contentDescription = null) },
                title = stringResource(R.string.tunnel_on_ethernet),
                trailing = { modifier ->
                    SwitchWithDivider(
                        checked = autoTunnelState.settings.isTunnelOnEthernetEnabled,
                        onClick = { viewModel.setTunnelOnEthernet(it) },
                        modifier = modifier,
                    )
                },
                description = {
                    Text(
                        buildAnnotatedString {
                            append(stringResource(R.string.preferred_tunnel_label))
                            ethernetTunnel?.tunName?.let { append(it) }
                                ?: withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(stringResource(R.string._default))
                                }
                        },
                        style =
                            MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.outline
                            ),
                    )
                },
                onClick = { navController.push(Route.PreferredTunnel(TunnelNetwork.ETHERNET)) },
            )
        }
        Column {
            GroupLabel(
                stringResource(R.string.other),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            SurfaceRow(
                leading = { Icon(Icons.Outlined.PublicOff, contentDescription = null) },
                title = stringResource(R.string.stop_on_no_internet),
                description = {
                    Text(
                        stringResource(R.string.stop_on_internet_loss),
                        style =
                            MaterialTheme.typography.bodySmall.copy(
                                MaterialTheme.colorScheme.outline
                            ),
                    )
                },
                trailing = {
                    ScaledSwitch(
                        checked = autoTunnelState.settings.isStopOnNoInternetEnabled,
                        onClick = { viewModel.setStopOnNoInternetEnabled(it) },
                    )
                },
                onClick = {
                    viewModel.setStopOnNoInternetEnabled(
                        !autoTunnelState.settings.isStopOnNoInternetEnabled
                    )
                },
            )
            SurfaceRow(
                leading = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                title = stringResource(R.string.advanced_settings),
                onClick = { navController.push(Route.AdvancedAutoTunnel) },
            )
        }
    }
}
