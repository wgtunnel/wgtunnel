package com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.enums.NetworkType
import com.zaneschepke.wireguardautotunnel.ui.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.ui.common.button.SwitchWithDivider
import com.zaneschepke.wireguardautotunnel.ui.common.functions.rememberClipboardHelper
import com.zaneschepke.wireguardautotunnel.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.ui.common.text.DescriptionText
import com.zaneschepke.wireguardautotunnel.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.ui.navigation.TunnelNetwork
import com.zaneschepke.wireguardautotunnel.viewmodel.AutoTunnelViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AutoTunnelScreen(viewModel: AutoTunnelViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val clipboard = rememberClipboardHelper()
    val autoTunnelState by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    if (autoTunnelState.isLoading) return

    val (ethernetTunnel, mobileDataTunnel, mappedTunnels) =
        remember(autoTunnelState.tunnels) {
            Triple(
                autoTunnelState.tunnels.firstOrNull { it.isEthernetTunnel },
                autoTunnelState.tunnels.firstOrNull { it.isMobileDataTunnel },
                autoTunnelState.tunnels.any { it.tunnelNetworks.isNotEmpty() },
            )
        }

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        Column {
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
            val activeNetworkType by
                remember(autoTunnelState.connectivityState) {
                    derivedStateOf {
                        val connectivity = autoTunnelState.connectivityState
                        when {
                            connectivity?.ethernetConnected == true -> NetworkType.ETHERNET
                            connectivity?.wifiState?.connected == true -> NetworkType.WIFI
                            connectivity?.cellularConnected == true -> NetworkType.MOBILE_DATA
                            else -> NetworkType.NONE
                        }
                    }
                }

            val localizedNetworkType =
                when (activeNetworkType) {
                    NetworkType.WIFI -> stringResource(R.string.wifi)
                    NetworkType.ETHERNET -> stringResource(R.string.ethernet)
                    NetworkType.MOBILE_DATA -> stringResource(R.string.mobile_data)
                    NetworkType.NONE -> stringResource(R.string.no_network)
                }

            val ssid by
                remember(autoTunnelState.connectivityState) {
                    derivedStateOf {
                        autoTunnelState.connectivityState?.wifiState?.ssid
                            ?: context.getString(R.string.unknown)
                    }
                }

            SurfaceRow(
                leading = {
                    Icon(ImageVector.vectorResource(R.drawable.globe), contentDescription = null)
                },
                title =
                    buildAnnotatedString {
                        append(stringResource(R.string.active_network))
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(localizedNetworkType)
                        }
                    },
                description =
                    if (activeNetworkType == NetworkType.WIFI) {
                        {
                            Column {
                                DescriptionText(
                                    buildAnnotatedString {
                                        append(stringResource(R.string.security_type))
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                            append(
                                                autoTunnelState.connectivityState
                                                    ?.wifiState
                                                    ?.securityType
                                                    ?.name ?: stringResource(R.string.unknown)
                                            )
                                        }
                                    }
                                )
                                DescriptionText(
                                    buildAnnotatedString {
                                        append(stringResource(R.string.network_name))
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                            append(ssid)
                                        }
                                    }
                                )
                            }
                        }
                    } else null,
                trailing =
                    if (activeNetworkType == NetworkType.WIFI) {
                        { Icon(Icons.Outlined.CopyAll, contentDescription = null) }
                    } else null,
                onClick =
                    if (activeNetworkType == NetworkType.WIFI) {
                        { clipboard.copy(ssid, context.getString(R.string.wifi)) }
                    } else null,
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
                    DescriptionText(
                        buildAnnotatedString {
                            append(stringResource(R.string.preferred_tunnel_label))
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(
                                    if (mappedTunnels) stringResource(R.string.mapped)
                                    else stringResource(R.string._default)
                                )
                            }
                        }
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
                    DescriptionText(
                        buildAnnotatedString {
                            append(stringResource(R.string.preferred_tunnel_label))
                            mobileDataTunnel?.tunName?.let { append(it) }
                                ?: withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(stringResource(R.string._default))
                                }
                        }
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
                    DescriptionText(
                        buildAnnotatedString {
                            append(stringResource(R.string.preferred_tunnel_label))
                            ethernetTunnel?.tunName?.let { append(it) }
                                ?: withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(stringResource(R.string._default))
                                }
                        }
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
