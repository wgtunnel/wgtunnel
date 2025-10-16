package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState
import com.zaneschepke.wireguardautotunnel.ui.common.ExpandingRowListItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.util.extensions.asColor

@Composable
fun TunnelRowItem(
    state: TunnelState,
    isSelected: Boolean,
    tunnel: TunnelConfig,
    onTvClick: () -> Unit,
    onToggleSelectedTunnel: (TunnelConfig) -> Unit,
    onSwitchClick: (Boolean) -> Unit,
    isTv: Boolean,
    isPingEnabled: Boolean,
    showDetailedStats: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var leadingIconColor by
        remember(state.status, state.logHealthState, state.pingStates, state.statistics) {
            mutableStateOf(state.health().asColor())
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
                        context.getString(R.string.mobile_tunnel),
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

    val statusDescription =
        remember(state) {
            if (state.status.isUpOrStarting()) {
                context.getString(R.string.active)
            } else {
                context.getString(R.string.inactive)
            }
        }

    val combinedContentDescription =
        stringResource(
            R.string.tunnel_item_description,
            tunnel.name,
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
        text = tunnel.name,
        expanded = {
            if (state.status != TunnelStatus.Down) {
                TunnelStatisticsRow(state, isPingEnabled, showDetailedStats)
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
