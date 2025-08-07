package com.zaneschepke.wireguardautotunnel.ui.screens.main.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.model.AppSettings
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
    appSettings: AppSettings,
    onTvClick: () -> Unit,
    onToggleSelectedTunnel: (TunnelConf) -> Unit,
    onSwitchClick: (Boolean) -> Unit,
    isTv: Boolean,
    showDetailedStats: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val leadingIconColor =
        remember(state) {
            if (state.status.isUp()) tunnelState.statistics.asColor() else Color.Gray
        }

    val (leadingIcon, size, typeDescription) =
        remember(tunnel) {
            when {
                tunnel.isPrimaryTunnel ->
                    Triple(Icons.Rounded.Star, 16.dp, context.getString(R.string.primary_tunnel))
                tunnel.isMobileDataTunnel ->
                    Triple(
                        Icons.Rounded.Smartphone,
                        16.dp,
                        context.getString(R.string.mobile_data_tunnel),
                    )
                tunnel.isEthernetTunnel ->
                    Triple(
                        Icons.Rounded.SettingsEthernet,
                        16.dp,
                        context.getString(R.string.ethernet_tunnel),
                    )
                else -> Triple(Icons.Rounded.Circle, 14.dp, context.getString(R.string.tunnel))
            }
        }

    // Status description based on tunnel state
    val statusDescription =
        remember(state) {
            if (state.status.isUpOrStarting()) {
                context.getString(R.string.active)
            } else {
                context.getString(R.string.inactive)
            }
        }

    // Combined content description for accessibility
    val combinedContentDescription =
        stringResource(
            R.string.tunnel_item_description,
            tunnel.tunName,
            typeDescription,
            statusDescription,
        )

    ExpandingRowListItem(
        modifier = modifier.semantics(mergeDescendants = true) { combinedContentDescription },
        leading = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
            ) {
                if (isTv) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelectedTunnel(tunnel) },
                        modifier = Modifier.minimumInteractiveComponentSize().size(12.dp),
                    )
                }
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    tint = leadingIconColor,
                    modifier = Modifier.size(size),
                )
            }
        },
        text = tunnel.tunName,
        expanded = {
            if (tunnelState.status != TunnelStatus.Down) {
                TunnelStatisticsRow(
                    tunnelState,
                    tunnel,
                    appSettings.isPingEnabled,
                    showDetailedStats,
                )
            }
        },
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                if (isTv) {
                    IconButton(onClick = onTvClick) {
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
