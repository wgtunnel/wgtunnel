package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.tunneloptions.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItemLabel
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionLabelType

@Composable
fun dynamicDnsItem(enabled: Boolean, onClick: (checked: Boolean) -> Unit): SelectionItem {
    return SelectionItem(
        leading = { Icon(Icons.Outlined.Dns, contentDescription = null) },
        title = {
            SelectionItemLabel(
                stringResource(R.string.ddns_auto_update),
                labelType = SelectionLabelType.TITLE,
            )
        },
        description = {
            SelectionItemLabel(
                stringResource(R.string.ddns_auto_update_description),
                labelType = SelectionLabelType.DESCRIPTION,
            )
        },
        trailing = { ScaledSwitch(checked = enabled, onClick = onClick) },
        onClick = { onClick(!enabled) },
    )
}
