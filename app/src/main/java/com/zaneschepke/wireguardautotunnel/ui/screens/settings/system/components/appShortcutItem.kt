package com.zaneschepke.wireguardautotunnel.ui.screens.settings.system.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem

@Composable
fun appShortcutsItem(checked: Boolean, onChange: (checked: Boolean) -> Unit): SelectionItem {
    return SelectionItem(
        leading = { Icon(Icons.Filled.AppShortcut, contentDescription = null) },
        trailing = { ScaledSwitch(checked = checked, onClick = onChange) },
        title = {
            Text(
                text = stringResource(R.string.enabled_app_shortcuts),
                style =
                    MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
            )
        },
        onClick = { onChange(!checked) },
    )
}
