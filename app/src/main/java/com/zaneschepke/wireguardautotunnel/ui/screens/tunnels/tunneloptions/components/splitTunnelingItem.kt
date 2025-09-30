package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.tunneloptions.components

import android.R.attr.onClick
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import com.zaneschepke.wireguardautotunnel.ui.common.button.ForwardButton
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItemLabel
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionLabelType

@Composable
fun splitTunnelingItem(label: String, onClick: () -> Unit): SelectionItem {
    return SelectionItem(
        leading = { Icon(Icons.AutoMirrored.Outlined.CallSplit, contentDescription = null) },
        title = { SelectionItemLabel(label, SelectionLabelType.TITLE) },
        trailing = { ForwardButton { onClick() } },
        onClick = onClick,
    )
}
