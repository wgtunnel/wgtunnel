package com.zaneschepke.wireguardautotunnel.ui.screens.main.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsEthernet
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState
import com.zaneschepke.wireguardautotunnel.ui.common.ExpandingRowListItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.util.extensions.asColor

@Composable
fun TunnelRowItem(
    state: TunnelState,
    isSelected: Boolean,
    tunnel: TunnelConf,
    tunnelState: TunnelState,
    onClick: () -> Unit,
    onToggleSelectedTunnel: (TunnelConf) -> Unit,
    onSwitchClick: (Boolean) -> Unit,
    isTv: Boolean,
) {
    val leadingIconColor =
        remember(state) {
            if (state.status.isUp()) tunnelState.statistics.asColor() else Color.Gray
        }

    val (leadingIcon, size) =
        remember(tunnel) {
            when {
                tunnel.isPrimaryTunnel -> Pair(Icons.Rounded.Star, 16.dp)
                tunnel.isMobileDataTunnel -> Pair(Icons.Rounded.Smartphone, 16.dp)
                tunnel.isEthernetTunnel -> Pair(Icons.Rounded.SettingsEthernet, 16.dp)
                else -> Pair(Icons.Rounded.Circle, 14.dp)
            }
        }

    ExpandingRowListItem(
        leading = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
            ) {
                if (isTv) {
                    Checkbox(
                        isSelected,
                        onCheckedChange = { onToggleSelectedTunnel(tunnel) },
                        modifier = Modifier.minimumInteractiveComponentSize().size(12.dp),
                    )
                }
                Icon(
                    leadingIcon,
                    stringResource(R.string.status),
                    tint = leadingIconColor,
                    modifier = Modifier.size(size),
                )
            }
        },
        text = tunnel.tunName,
        onHold = { if (!isTv) onToggleSelectedTunnel(tunnel) },
        onClick = { if (!isTv) onClick() },
        expanded = {
            if (tunnelState.status != TunnelStatus.Down) {
                TunnelStatisticsRow(tunnelState.statistics, tunnel)
            }
        },
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                if (isTv) {
                    IconButton(onClick = onClick) {
                        Icon(
                            Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.settings),
                        )
                    }
                }
                ScaledSwitch(checked = state.status.isUpOrStarting(), onClick = onSwitchClick)
            }
        },
        isSelected = isSelected,
    )
}
