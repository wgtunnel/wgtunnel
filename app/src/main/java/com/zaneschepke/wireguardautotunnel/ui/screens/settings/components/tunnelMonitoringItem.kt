package com.zaneschepke.wireguardautotunnel.ui.screens.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.button.ForwardButton
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.navigation.LocalNavController

@Composable
fun tunnelMonitoringItem(): SelectionItem {
    val navController = LocalNavController.current
    return SelectionItem(
        leading = { Icon(Icons.Outlined.MonitorHeart, null) },
        trailing = { ForwardButton { navController.navigate(Route.TunnelMonitoring) } },
        title = {
            Text(
                text = stringResource(R.string.tunnel_monitoring),
                style =
                    MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
            )
        },
        onClick = { navController.navigate(Route.TunnelMonitoring) },
    )
}
