package com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.disclosure.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.button.ForwardButton
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.util.extensions.goFromRoot

@Composable
fun skipItem(): SelectionItem {
    val navController = LocalNavController.current
    return SelectionItem(
        title = {
            Text(
                text = stringResource(R.string.skip),
                style = MaterialTheme.typography.bodyLarge.copy(MaterialTheme.colorScheme.onSurface),
            )
        },
        trailing = { ForwardButton { navController.goFromRoot(Route.AutoTunnel) } },
        onClick = { navController.goFromRoot(Route.AutoTunnel) },
    )
}
