package com.zaneschepke.wireguardautotunnel.ui.screens.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VpnKeyOff
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
fun killSwitchItem(): SelectionItem {
    val navController = LocalNavController.current
    return SelectionItem(
        leading = { Icon(Icons.Outlined.VpnKeyOff, contentDescription = null) },
        title = {
            Text(
                text = stringResource(R.string.kill_switch_options),
                style =
                    MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
            )
        },
        trailing = { ForwardButton { navController.navigate(Route.KillSwitch) } },
        onClick = { navController.navigate(Route.KillSwitch) },
    )
}
