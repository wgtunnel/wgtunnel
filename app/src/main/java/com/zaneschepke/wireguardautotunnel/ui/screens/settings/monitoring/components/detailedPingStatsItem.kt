package com.zaneschepke.wireguardautotunnel.ui.screens.settings.monitoring.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem

@Composable
fun detailedPingStatsItem(
    detailedPingStatsEnabled: Boolean,
    onChange: (checked: Boolean) -> Unit,
): SelectionItem {
    return SelectionItem(
        leading = { Icon(Icons.Outlined.QueryStats, contentDescription = null) },
        title = {
            Text(
                text = stringResource(R.string.display_detailed_ping_stats),
                style =
                    MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
            )
        },
        trailing = { ScaledSwitch(checked = detailedPingStatsEnabled, onClick = onChange) },
        onClick = { onChange(!detailedPingStatsEnabled) },
    )
}
