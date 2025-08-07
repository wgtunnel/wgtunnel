package com.zaneschepke.wireguardautotunnel.ui.screens.settings.monitoring.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NetworkPing
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
fun enablePingMonitoringItem(uiState: AppUiState, viewModel: AppViewModel): SelectionItem {
    return SelectionItem(
        leading = { Icon(Icons.Outlined.NetworkPing, contentDescription = null) },
        trailing = {
            ScaledSwitch(
                checked = uiState.appSettings.isPingEnabled,
                onClick = { viewModel.handleEvent(AppEvent.TogglePingMonitoring) },
            )
        },
        title = {
            Text(
                text = stringResource(R.string.monitoring_ping),
                style =
                    MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
            )
        },
        onClick = { viewModel.handleEvent(AppEvent.TogglePingMonitoring) },
    )
}
