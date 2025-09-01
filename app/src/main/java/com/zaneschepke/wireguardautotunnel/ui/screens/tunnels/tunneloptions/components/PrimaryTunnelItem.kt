package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.tunneloptions.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem

@Composable
fun PrimaryTunnelItem(tunnelConf: TunnelConf, onClick: () -> Unit): SelectionItem {
    return SelectionItem(
        leading = { Icon(Icons.Outlined.Star, contentDescription = null) },
        title = {
            Text(
                text = stringResource(R.string.primary_tunnel),
                style =
                    MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
            )
        },
        description = {
            Text(
                text = stringResource(R.string.set_primary_tunnel),
                style = MaterialTheme.typography.bodySmall.copy(MaterialTheme.colorScheme.outline),
            )
        },
        trailing = { ScaledSwitch(checked = tunnelConf.isPrimaryTunnel, onClick = { onClick() }) },
        onClick = onClick,
    )
}
