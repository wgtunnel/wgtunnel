package com.zaneschepke.wireguardautotunnel.ui.screens.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VpnLock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun alwaysOnVpnItem(uiState: AppUiState, viewModel: AppViewModel): SelectionItem {
    return SelectionItem(
        leading = { Icon(Icons.Outlined.VpnLock, contentDescription = null) },
        trailing = {
            ScaledSwitch(
                enabled =
                    !((uiState.appSettings.isTunnelOnWifiEnabled ||
                        uiState.appSettings.isTunnelOnEthernetEnabled ||
                        uiState.appSettings.isTunnelOnMobileDataEnabled) &&
                        uiState.appSettings.isAutoTunnelEnabled),
                checked = uiState.appSettings.isAlwaysOnVpnEnabled,
                onClick = { viewModel.handleEvent(AppEvent.ToggleAlwaysOn) },
            )
        },
        title = {
            Text(
                text = stringResource(R.string.always_on_vpn_support),
                style =
                    MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
            )
        },
        onClick = { viewModel.handleEvent(AppEvent.ToggleAlwaysOn) },
    )
}
