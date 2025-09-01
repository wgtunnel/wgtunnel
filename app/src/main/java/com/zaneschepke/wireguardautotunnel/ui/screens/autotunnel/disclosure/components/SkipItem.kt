package com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.disclosure.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ForwardButton
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.navigation.Route

@Composable
fun skipItem(navController: NavController): SelectionItem {
    return SelectionItem(
        title = {
            Text(
                text = stringResource(R.string.skip),
                style = MaterialTheme.typography.bodyLarge.copy(MaterialTheme.colorScheme.onSurface),
            )
        },
        trailing = { ForwardButton { navController.navigate(Route.AutoTunnelGraph) } },
        onClick = { navController.navigate(Route.AutoTunnelGraph) },
    )
}
