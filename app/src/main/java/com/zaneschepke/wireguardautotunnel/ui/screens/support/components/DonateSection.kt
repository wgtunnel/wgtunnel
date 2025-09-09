package com.zaneschepke.wireguardautotunnel.ui.screens.support.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ForwardButton
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItemLabel
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionLabelType
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton

@Composable
fun DonateSection(onClick: () -> Unit) {
    SurfaceSelectionGroupButton(
        listOf(
            SelectionItem(
                leading = { Icon(Icons.Outlined.Favorite, contentDescription = null) },
                title = {
                    SelectionItemLabel(stringResource(R.string.donate), SelectionLabelType.TITLE)
                },
                trailing = { ForwardButton { onClick() } },
                onClick = onClick,
            )
        )
    )
}
