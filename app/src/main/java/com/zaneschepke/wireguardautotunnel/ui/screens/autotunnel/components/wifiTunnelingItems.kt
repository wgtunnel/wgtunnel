package com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Filter1
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiFind
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ForwardButton
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.functions.rememberClipboardHelper
import com.zaneschepke.wireguardautotunnel.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.LearnMoreLinkLabel
import com.zaneschepke.wireguardautotunnel.ui.state.AutoTunnelUiState
import com.zaneschepke.wireguardautotunnel.ui.theme.iconSize
import com.zaneschepke.wireguardautotunnel.util.extensions.asTitleString
import com.zaneschepke.wireguardautotunnel.util.extensions.openWebUrl
import com.zaneschepke.wireguardautotunnel.viewmodel.AutoTunnelViewModel

@Composable
fun wifiTunnelingItems(
    autoTunnelState: AutoTunnelUiState,
    viewModel: AutoTunnelViewModel,
    navController: NavController,
): List<SelectionItem> {
    val context = LocalContext.current
    val clipboardHelper = rememberClipboardHelper()

    var currentText by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(autoTunnelState.generalSettings.trustedNetworkSSIDs) { currentText = "" }

    val baseItems =
        listOf(
            SelectionItem(
                leading = { Icon(Icons.Outlined.Wifi, contentDescription = null) },
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
                        checked = autoTunnelState.generalSettings.isTunnelOnWifiEnabled,
                        onClick = { viewModel.setAutoTunnelOnWifiEnabled(it) },
                    )
                },
                description = {
                    val wifiInfo by
                        remember(autoTunnelState.connectivityState) {
                            derivedStateOf {
                                autoTunnelState.connectivityState
                                    ?.wifiState
                                    ?.takeIf { it.connected }
                                    .let { Pair(it?.ssid, it?.securityType) }
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
                onClick = {
                    viewModel.setAutoTunnelOnWifiEnabled(
                        !autoTunnelState.generalSettings.isTunnelOnWifiEnabled
                    )
                },
            )
        )

    return if (autoTunnelState.generalSettings.isTunnelOnWifiEnabled) {
        baseItems +
            listOf(
                SelectionItem(
                    leading = { Icon(Icons.Outlined.WifiFind, contentDescription = null) },
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
                                autoTunnelState.generalSettings.wifiDetectionMethod.asTitleString(
                                    context
                                ),
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
                    leading = { Icon(Icons.Outlined.Filter1, contentDescription = null) },
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
                            checked = autoTunnelState.generalSettings.isWildcardsEnabled,
                            onClick = { viewModel.setWildcardsEnabled(it) },
                        )
                    },
                    onClick = {
                        viewModel.setWildcardsEnabled(
                            !autoTunnelState.generalSettings.isWildcardsEnabled
                        )
                    },
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
                            autoTunnelState.generalSettings.trustedNetworkSSIDs,
                            onDelete = { viewModel.removeTrustedNetworkName(it) },
                            currentText = currentText,
                            onSave = { ssid -> viewModel.saveTrustedNetworkName(ssid) },
                            onValueChange = { currentText = it },
                            supporting = {
                                if (autoTunnelState.generalSettings.isWildcardsEnabled)
                                    WildcardsLabel()
                            },
                        )
                    },
                ),
            )
    } else {
        baseItems
    }
}
