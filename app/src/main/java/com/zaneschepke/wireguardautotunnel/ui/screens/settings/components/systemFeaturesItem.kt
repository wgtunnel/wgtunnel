package com.zaneschepke.wireguardautotunnel.ui.screens.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ForwardButton
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem

@Composable
fun systemFeaturesItem(onClick: () -> Unit): SelectionItem {
    return SelectionItem(
        leading = { Icon(Icons.Outlined.Android, null) },
        trailing = { ForwardButton { onClick.invoke() } },
        title = {
            Text(
                text = stringResource(R.string.system_features),
                style =
                    MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
            )
        },
        onClick = onClick,
    )
}
