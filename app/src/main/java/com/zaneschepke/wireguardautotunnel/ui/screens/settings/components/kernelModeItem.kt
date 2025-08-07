package com.zaneschepke.wireguardautotunnel.ui.screens.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
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
fun kernelModeItem(uiState: AppUiState, viewModel: AppViewModel): SelectionItem {
    return SelectionItem(
        leading = { Icon(Icons.Outlined.Code, contentDescription = null) },
        title = {
            Text(
                text = stringResource(R.string.kernel),
                style =
                    MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
            )
        },
        description = {
            Text(
                text = stringResource(R.string.use_kernel),
                style = MaterialTheme.typography.bodySmall.copy(MaterialTheme.colorScheme.outline),
            )
        },
        trailing = {
            ScaledSwitch(
                checked = uiState.appSettings.isKernelEnabled,
                enabled =
                    !(uiState.appSettings.isAutoTunnelEnabled ||
                        uiState.appSettings.isAlwaysOnVpnEnabled ||
                        uiState.activeTunnels.isNotEmpty()),
                onClick = { viewModel.handleEvent(AppEvent.ToggleKernelMode) },
            )
        },
        onClick = { viewModel.handleEvent(AppEvent.ToggleKernelMode) },
    )
}
