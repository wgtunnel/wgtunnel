package com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.disclosure.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.LaunchButton
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItemLabel
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionLabelType

@Composable
fun appSettingsItem(onClick: () -> Unit): SelectionItem {
    return SelectionItem(
        leading = { Icon(Icons.Outlined.LocationOn, contentDescription = null) },
        title = {
            SelectionItemLabel(
                stringResource(R.string.launch_app_settings),
                labelType = SelectionLabelType.TITLE,
            )
        },
        trailing = { LaunchButton { onClick() } },
        onClick = onClick,
    )
}
