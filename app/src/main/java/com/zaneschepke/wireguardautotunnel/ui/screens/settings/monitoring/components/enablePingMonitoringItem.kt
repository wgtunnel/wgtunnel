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

@Composable
fun enablePingMonitoringItem(
    isPingEnabled: Boolean,
    onChange: (checked: Boolean) -> Unit,
): SelectionItem {
    return SelectionItem(
        leading = { Icon(Icons.Outlined.NetworkPing, contentDescription = null) },
        trailing = { ScaledSwitch(checked = isPingEnabled, onClick = onChange) },
        title = {
            Text(
                text = stringResource(R.string.monitoring_ping),
                style =
                    MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
            )
        },
        onClick = { onChange(!isPingEnabled) },
    )
}
