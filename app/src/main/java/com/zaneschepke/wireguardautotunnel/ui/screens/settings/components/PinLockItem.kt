package com.zaneschepke.wireguardautotunnel.ui.screens.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem

@Composable
fun pinLockItem(isPinLockEnabled: Boolean, onClick: (checked: Boolean) -> Unit): SelectionItem {

    return SelectionItem(
        leading = { Icon(Icons.Outlined.Pin, contentDescription = null) },
        title = {
            Text(
                text = stringResource(R.string.enable_app_lock),
                style =
                    MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
            )
        },
        trailing = { ScaledSwitch(checked = isPinLockEnabled, onClick = onClick) },
        onClick = { onClick(!isPinLockEnabled) },
    )
}
