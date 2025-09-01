package com.zaneschepke.wireguardautotunnel.ui.screens.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewTimeline
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ForwardButton
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItemLabel
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionLabelType
import com.zaneschepke.wireguardautotunnel.ui.navigation.Route

@Composable
fun readLogsItem(navController: NavController): SelectionItem {
    return SelectionItem(
        leading = { Icon(Icons.Filled.ViewTimeline, contentDescription = null) },
        title = {
            SelectionItemLabel(stringResource(R.string.read_logs), SelectionLabelType.TITLE)
        },
        trailing = { ForwardButton { navController.navigate(Route.Logs) } },
        onClick = { navController.navigate(Route.Logs) },
    )
}
