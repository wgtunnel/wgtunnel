package com.zaneschepke.wireguardautotunnel.ui.screens.settings.killswitch.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState

@Composable
fun VpnKillSwitchItem(uiState: AppUiState, toggleVpnSwitch: () -> Unit): SelectionItem {
    return SelectionItem(
        leading = { Icon(Icons.Outlined.VpnKey, contentDescription = null) },
        title = {
            Text(
                text = stringResource(R.string.vpn_kill_switch),
                style =
                    MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
            )
        },
        trailing = {
            ScaledSwitch(
                checked = uiState.appSettings.isVpnKillSwitchEnabled,
                onClick = { toggleVpnSwitch() },
            )
        },
        onClick = { toggleVpnSwitch() },
    )
}
