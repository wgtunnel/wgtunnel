package com.zaneschepke.wireguardautotunnel.ui.screens.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ViewHeadline
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItemLabel
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionLabelType

@Composable
fun localLoggingItem(
    isLocalLoggingEnabled: Boolean,
    onClick: (checked: Boolean) -> Unit,
): SelectionItem {
    return SelectionItem(
        leading = { Icon(Icons.Outlined.ViewHeadline, contentDescription = null) },
        title = {
            SelectionItemLabel(stringResource(R.string.local_logging), SelectionLabelType.TITLE)
        },
        description = {
            SelectionItemLabel(
                stringResource(R.string.enable_local_logging),
                SelectionLabelType.DESCRIPTION,
            )
        },
        trailing = { ScaledSwitch(checked = isLocalLoggingEnabled, onClick = onClick) },
        onClick = { onClick(!isLocalLoggingEnabled) },
    )
}
