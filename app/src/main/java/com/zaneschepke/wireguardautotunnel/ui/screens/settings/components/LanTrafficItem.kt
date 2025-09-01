package com.zaneschepke.wireguardautotunnel.ui.screens.settings.components

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

@Composable
fun lanTrafficItem(
    isLanKillSwitchEnabled: Boolean,
    onClick: (enable: Boolean) -> Unit,
): SelectionItem {
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
        trailing = { ScaledSwitch(checked = isLanKillSwitchEnabled, onClick = onClick) },
        onClick = { onClick(!isLanKillSwitchEnabled) },
    )
}
