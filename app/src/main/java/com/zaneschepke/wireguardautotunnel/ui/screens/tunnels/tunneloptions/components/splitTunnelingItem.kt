package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.tunneloptions.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.ui.common.button.ForwardButton
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.navigation.Route

@Composable
fun splitTunnelingItem(tunnelConf: TunnelConf, navController: NavController): SelectionItem {
    return SelectionItem(
        leading = { Icon(Icons.AutoMirrored.Outlined.CallSplit, contentDescription = null) },
        title = {
            Text(
                text = stringResource(R.string.splt_tunneling),
                style =
                    MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
            )
        },
        trailing = {
            ForwardButton { navController.navigate(Route.SplitTunnel(id = tunnelConf.id)) }
        },
        onClick = { navController.navigate(Route.SplitTunnel(id = tunnelConf.id)) },
    )
}
