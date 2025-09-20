package com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.disclosure.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ForwardButton
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItemLabel
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionLabelType

@Composable
fun skipItem(onClick: () -> Unit): SelectionItem {
    return SelectionItem(
        title = {
            SelectionItemLabel(stringResource(R.string.skip), labelType = SelectionLabelType.TITLE)
        },
        trailing = { ForwardButton { onClick() } },
        onClick = onClick,
    )
}
