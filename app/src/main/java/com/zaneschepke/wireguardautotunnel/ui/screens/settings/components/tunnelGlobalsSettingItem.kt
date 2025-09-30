package com.zaneschepke.wireguardautotunnel.ui.screens.settings.components

import android.R.attr.enabled
import android.R.attr.onClick
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.BlendMode.Companion.Screen
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItemLabel
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionLabelType
import com.zaneschepke.wireguardautotunnel.ui.navigation.Route

@Composable
fun tunnelGlobalsSettingItem(enabled: Boolean, onClick: (Boolean) -> Unit, onItemClick: () -> Unit) : SelectionItem {
    return SelectionItem(
        leading = { Icon(ImageVector.vectorResource(R.drawable.globe), contentDescription = null) },
        title = {
            SelectionItemLabel(
                stringResource(R.string.tunnel_global_overrides),
                SelectionLabelType.TITLE
            )
        },
        trailing = { ScaledSwitch(checked = enabled, onClick = onClick) },
        onClick = onItemClick,
    )
}