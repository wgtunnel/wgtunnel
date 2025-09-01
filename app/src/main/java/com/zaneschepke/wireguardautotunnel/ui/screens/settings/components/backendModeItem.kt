package com.zaneschepke.wireguardautotunnel.ui.screens.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItemLabel
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionLabelType
import com.zaneschepke.wireguardautotunnel.util.extensions.asTitleString

@Composable
fun backendModeItem(appMode: AppMode, onClick: () -> Unit): SelectionItem {
    val context = LocalContext.current
    return SelectionItem(
        leading = { Icon(ImageVector.vectorResource(R.drawable.sdk), contentDescription = null) },
        trailing = {
            Icon(Icons.Outlined.ExpandMore, contentDescription = stringResource(R.string.select))
        },
        title = {
            SelectionItemLabel(stringResource(R.string.backend_mode), SelectionLabelType.TITLE)
        },
        description = {
            SelectionItemLabel(
                stringResource(R.string.current_template, appMode.asTitleString(context)),
                SelectionLabelType.DESCRIPTION,
            )
        },
        onClick = onClick,
    )
}
