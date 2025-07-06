package com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zaneschepke.networkmonitor.AndroidNetworkMonitor
import com.zaneschepke.networkmonitor.NetworkStatus
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.button.ForwardButton
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.functions.rememberClipboardHelper
import com.zaneschepke.wireguardautotunnel.ui.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.LearnMoreLinkLabel
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.ui.theme.iconSize
import com.zaneschepke.wireguardautotunnel.util.extensions.asString
import com.zaneschepke.wireguardautotunnel.util.extensions.openWebUrl
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun WifiTunnelingItems(
    uiState: AppUiState,
    viewModel: AppViewModel,
    currentText: String,
    onTextChange: (String) -> Unit,
    isWifiNameReadable: () -> Boolean,
): List<SelectionItem> {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val clipboardHelper = rememberClipboardHelper()

    val baseItems =
        listOf(
            SelectionItem(
                leadingIcon = Icons.Outlined.Wifi,
                title = {
                    Text(
                        stringResource(R.string.tunnel_on_wifi),
                        style =
                            MaterialTheme.typography.bodyMedium.copy(
                                MaterialTheme.colorScheme.onSurface
                            ),
                    )
                },
                trailing = {
                    ScaledSwitch(
                        enabled = !uiState.appSettings.isAlwaysOnVpnEnabled,
                        checked = uiState.appSettings.isTunnelOnWifiEnabled,
                        onClick = { viewModel.handleEvent(AppEvent.ToggleAutoTunnelOnWifi) },
                    )
                },
                description = {
                    val wifiInfo by
                        remember(uiState.networkStatus) {
                            derivedStateOf {
                                (uiState.networkStatus as? NetworkStatus.Connected)
                                    ?.takeIf { it.wifiConnected }
                                    .let { Pair(it?.wifiSsid, it?.securityType) }
                            }
                        }
                    val (wifiName, securityType) = wifiInfo
                    Column {
                        Text(
                            text =
                                wifiName?.let { stringResource(R.string.wifi_name_template, it) }
                                    ?: stringResource(R.string.inactive),
                            style =
                                MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.outline
                                ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier =
                                Modifier.clickable { wifiName?.let { clipboardHelper.copy(it) } },
                        )
                        securityType?.let {
                            Text(
                                text = stringResource(R.string.security_template, it.name),
                                style =
                                    MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.outline
                                    ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                onClick = { viewModel.handleEvent(AppEvent.ToggleAutoTunnelOnWifi) },
            )
        )

    return if (uiState.appSettings.isTunnelOnWifiEnabled) {
        baseItems +
            listOf(
                SelectionItem(
                    leadingIcon = Icons.Outlined.WifiFind,
                    title = {
                        Text(
                            stringResource(R.string.wifi_detection_method),
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    MaterialTheme.colorScheme.onSurface
                                ),
                        )
                    },
                    description = {
                        Text(
                            stringResource(
                                R.string.current_template,
                                uiState.appSettings.wifiDetectionMethod.asString(context),
                            ),
                            style =
                                MaterialTheme.typography.bodySmall.copy(
                                    MaterialTheme.colorScheme.outline
                                ),
                        )
                    },
                    trailing = {
                        ForwardButton { navController.navigate(Route.WifiDetectionMethod) }
                    },
                    onClick = { navController.navigate(Route.WifiDetectionMethod) },
                ),
                SelectionItem(
                    leadingIcon = Icons.Outlined.Filter1,
                    title = {
                        Text(
                            stringResource(R.string.use_wildcards),
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    MaterialTheme.colorScheme.onSurface
                                ),
                        )
                    },
                    description = {
                        LearnMoreLinkLabel(
                            { context.openWebUrl(it) },
                            stringResource(R.string.docs_wildcards),
                        )
                    },
                    trailing = {
                        ScaledSwitch(
                            checked = uiState.appSettings.isWildcardsEnabled,
                            onClick = { viewModel.handleEvent(AppEvent.ToggleAutoTunnelWildcards) },
                        )
                    },
                    onClick = { viewModel.handleEvent(AppEvent.ToggleAutoTunnelWildcards) },
                ),
                SelectionItem(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(4f, false).fillMaxWidth(),
                            ) {
                                val icon = Icons.Outlined.Security
                                Icon(icon, icon.name, modifier = Modifier.size(iconSize))
                                Column(
                                    horizontalAlignment = Alignment.Start,
                                    verticalArrangement =
                                        Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .padding(start = 16.dp)
                                            .padding(vertical = 6.dp),
                                ) {
                                    Text(
                                        stringResource(R.string.trusted_wifi_names),
                                        style =
                                            MaterialTheme.typography.bodyMedium.copy(
                                                MaterialTheme.colorScheme.onSurface
                                            ),
                                    )
                                }
                            }
                        }
                    },
                    description = {
                        TrustedNetworkTextBox(
                            uiState.appSettings.trustedNetworkSSIDs,
                            onDelete = { viewModel.handleEvent(AppEvent.DeleteTrustedSSID(it)) },
                            currentText = currentText,
                            onSave = { ssid ->
                                if (
                                    uiState.appSettings.wifiDetectionMethod ==
                                        AndroidNetworkMonitor.WifiDetectionMethod.ROOT ||
                                        uiState.appSettings.wifiDetectionMethod ==
                                            AndroidNetworkMonitor.WifiDetectionMethod.SHIZUKU ||
                                        isWifiNameReadable()
                                ) {
                                    viewModel.handleEvent(AppEvent.SaveTrustedSSID(ssid))
                                }
                            },
                            onValueChange = onTextChange,
                            supporting = {
                                if (uiState.appSettings.isWildcardsEnabled) WildcardsLabel()
                            },
                        )
                    },
                ),
                SelectionItem(
                    leadingIcon = Icons.Outlined.VpnKeyOff,
                    title = {
                        Text(
                            stringResource(R.string.kill_switch_off),
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    MaterialTheme.colorScheme.onSurface
                                ),
                        )
                    },
                    trailing = {
                        ScaledSwitch(
                            enabled = uiState.appSettings.isVpnKillSwitchEnabled,
                            checked = uiState.appSettings.isDisableKillSwitchOnTrustedEnabled,
                            onClick = {
                                viewModel.handleEvent(AppEvent.ToggleStopKillSwitchOnTrusted)
                            },
                        )
                    },
                    onClick = { viewModel.handleEvent(AppEvent.ToggleStopKillSwitchOnTrusted) },
                ),
            )
    } else {
        baseItems
    }
}
