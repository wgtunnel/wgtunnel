package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.autotunnel.components

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

@Composable
fun PingRestartItem(enabled: Boolean, onClick: (Boolean) -> Unit): SelectionItem {
    return SelectionItem(
        leading = { Icon(Icons.Outlined.NetworkPing, contentDescription = null) },
        title = {
            Text(
                text = stringResource(R.string.restart_on_ping),
                style =
                    MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
            )
        },
        trailing = { ScaledSwitch(checked = enabled, onClick = onClick) },
        onClick = { onClick(!enabled) },
    )
}
