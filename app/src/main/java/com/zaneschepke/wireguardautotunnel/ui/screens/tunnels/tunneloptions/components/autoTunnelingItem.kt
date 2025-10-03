package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.tunneloptions.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.ui.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.button.ForwardButton
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.navigation.Route

@Composable
fun autoTunnelingItem(tunnelConf: TunnelConf): SelectionItem {
    val navController = LocalNavController.current
    return SelectionItem(
        leading = { Icon(Icons.Outlined.Bolt, contentDescription = null) },
        title = {
            Text(
                text = stringResource(R.string.auto_tunneling),
                style =
                    MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
            )
        },
        description = {
            Text(
                text = stringResource(R.string.tunnel_specific_settings),
                style = MaterialTheme.typography.bodySmall.copy(MaterialTheme.colorScheme.outline),
            )
        },
        trailing = {
            ForwardButton { navController.push(Route.TunnelAutoTunnel(id = tunnelConf.id)) }
        },
        onClick = { navController.push(Route.TunnelAutoTunnel(id = tunnelConf.id)) },
    )
}
