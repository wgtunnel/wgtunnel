package com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.disclosure.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ForwardButton
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItemLabel
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionLabelType
import com.zaneschepke.wireguardautotunnel.util.extensions.launchAppSettings

@Composable
fun appSettingsItem(): SelectionItem {
    val context = LocalContext.current

    return SelectionItem(
        leading = { Icon(Icons.Outlined.LocationOn, contentDescription = null) },
        title = {
            SelectionItemLabel(
                stringResource(R.string.launch_app_settings),
                labelType = SelectionLabelType.TITLE,
            )
        },
        trailing = { ForwardButton { context.launchAppSettings() } },
        onClick = { context.launchAppSettings() },
    )
}
