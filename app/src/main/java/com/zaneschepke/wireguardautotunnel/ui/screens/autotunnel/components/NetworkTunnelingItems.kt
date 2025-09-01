package com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PublicOff
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material.icons.outlined.SignalCellular4Bar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.state.AutoTunnelUiState
import com.zaneschepke.wireguardautotunnel.viewmodel.AutoTunnelViewModel

@Composable
fun networkTunnelingItems(
    autoTunnelState: AutoTunnelUiState,
    viewModel: AutoTunnelViewModel,
): List<SelectionItem> {
    return listOf(
        SelectionItem(
            leading = { Icon(Icons.Outlined.SignalCellular4Bar, contentDescription = null) },
            title = {
                Text(
                    stringResource(R.string.tunnel_mobile_data),
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            MaterialTheme.colorScheme.onSurface
                        ),
                )
            },
            trailing = {
                ScaledSwitch(
                    enabled = !autoTunnelState.generalSettings.isAlwaysOnVpnEnabled,
                    checked = autoTunnelState.generalSettings.isTunnelOnMobileDataEnabled,
                    onClick = { viewModel.setTunnelOnCellular(it) },
                )
            },
            description = {
                val cellularActive =
                    remember(autoTunnelState.connectivityState) {
                        autoTunnelState.connectivityState?.cellularConnected ?: false
                    }
                Text(
                    text =
                        if (cellularActive) stringResource(R.string.active)
                        else stringResource(R.string.inactive),
                    style =
                        MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.outline
                        ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            onClick = {
                viewModel.setTunnelOnCellular(
                    !autoTunnelState.generalSettings.isTunnelOnMobileDataEnabled
                )
            },
        ),
        SelectionItem(
            leading = { Icon(Icons.Outlined.SettingsEthernet, contentDescription = null) },
            title = {
                Text(
                    stringResource(R.string.tunnel_on_ethernet),
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            MaterialTheme.colorScheme.onSurface
                        ),
                )
            },
            trailing = {
                ScaledSwitch(
                    enabled = !autoTunnelState.generalSettings.isAlwaysOnVpnEnabled,
                    checked = autoTunnelState.generalSettings.isTunnelOnEthernetEnabled,
                    onClick = { viewModel.setTunnelOnEthernet(it) },
                )
            },
            description = {
                val ethernetActive =
                    remember(autoTunnelState.connectivityState) {
                        autoTunnelState.connectivityState?.ethernetConnected ?: false
                    }
                Text(
                    text =
                        if (ethernetActive) stringResource(R.string.active)
                        else stringResource(R.string.inactive),
                    style =
                        MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.outline
                        ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            onClick = {
                viewModel.setTunnelOnEthernet(
                    !autoTunnelState.generalSettings.isTunnelOnEthernetEnabled
                )
            },
        ),
        SelectionItem(
            leading = { Icon(Icons.Outlined.PublicOff, contentDescription = null) },
            title = {
                Text(
                    stringResource(R.string.stop_on_no_internet),
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            MaterialTheme.colorScheme.onSurface
                        ),
                )
            },
            description = {
                Text(
                    stringResource(R.string.stop_on_internet_loss),
                    style =
                        MaterialTheme.typography.bodySmall.copy(MaterialTheme.colorScheme.outline),
                )
            },
            trailing = {
                ScaledSwitch(
                    checked = autoTunnelState.generalSettings.isStopOnNoInternetEnabled,
                    onClick = { viewModel.setStopOnNoInternetEnabled(it) },
                )
            },
            onClick = {
                viewModel.setStopOnNoInternetEnabled(
                    !autoTunnelState.generalSettings.isStopOnNoInternetEnabled
                )
            },
        ),
    )
}
