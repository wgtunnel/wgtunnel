package com.zaneschepke.wireguardautotunnel.ui.screens.settings.killswitch.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lan
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
fun LanTrafficItem(uiState: AppUiState, viewModel: AppViewModel): SelectionItem {
    return SelectionItem(
        leading = { Icon(Icons.Outlined.Lan, contentDescription = null) },
        title = {
            Text(
                text = stringResource(R.string.allow_lan_traffic),
                style =
                    MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
            )
        },
        description = {
            Text(
                text = stringResource(R.string.bypass_lan_for_kill_switch),
                style = MaterialTheme.typography.bodySmall.copy(MaterialTheme.colorScheme.outline),
            )
        },
        trailing = {
            ScaledSwitch(
                checked = uiState.appSettings.isLanOnKillSwitchEnabled,
                onClick = { viewModel.handleEvent(AppEvent.ToggleLanOnKillSwitch) },
            )
        },
        onClick = { viewModel.handleEvent(AppEvent.ToggleLanOnKillSwitch) },
    )
}
