package com.zaneschepke.wireguardautotunnel.ui.screens.settings.globals.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ForwardButton
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItemLabel
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionLabelType

@Composable
fun globalConfigItem(onClick: () -> Unit): SelectionItem {
    return SelectionItem(
        leading = { Icon(Icons.Outlined.Settings, contentDescription = null) },
        title = {
            SelectionItemLabel(stringResource(R.string.configuration), SelectionLabelType.TITLE)
        },
        trailing = { ForwardButton { onClick() } },
        onClick = onClick,
    )
}
