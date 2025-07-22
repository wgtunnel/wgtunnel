package com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PublicOff
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material.icons.outlined.SignalCellular4Bar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun NetworkTunnelingItems(uiState: AppUiState, viewModel: AppViewModel): List<SelectionItem> {
    return listOf(
        SelectionItem(
            leadingIcon = Icons.Outlined.SignalCellular4Bar,
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
                    enabled = !uiState.appSettings.isAlwaysOnVpnEnabled,
                    checked = uiState.appSettings.isTunnelOnMobileDataEnabled,
                    onClick = { viewModel.handleEvent(AppEvent.ToggleAutoTunnelOnCellular) },
                )
            },
            description = {
                val cellularActive =
                    remember(uiState.connectivityState) {
                        uiState.connectivityState?.cellularConnected ?: false
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
            onClick = { viewModel.handleEvent(AppEvent.ToggleAutoTunnelOnCellular) },
        ),
        SelectionItem(
            leadingIcon = Icons.Outlined.SettingsEthernet,
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
                    enabled = !uiState.appSettings.isAlwaysOnVpnEnabled,
                    checked = uiState.appSettings.isTunnelOnEthernetEnabled,
                    onClick = { viewModel.handleEvent(AppEvent.ToggleAutoTunnelOnEthernet) },
                )
            },
            description = {
                val ethernetActive =
                    remember(uiState.connectivityState) {
                        uiState.connectivityState?.ethernetConnected ?: false
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
            onClick = { viewModel.handleEvent(AppEvent.ToggleAutoTunnelOnEthernet) },
        ),
        SelectionItem(
            leadingIcon = Icons.Outlined.PublicOff,
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
                    checked = uiState.appSettings.isStopOnNoInternetEnabled,
                    onClick = { viewModel.handleEvent(AppEvent.ToggleStopTunnelOnNoInternet) },
                )
            },
            onClick = { viewModel.handleEvent(AppEvent.ToggleStopTunnelOnNoInternet) },
        ),
    )
}
