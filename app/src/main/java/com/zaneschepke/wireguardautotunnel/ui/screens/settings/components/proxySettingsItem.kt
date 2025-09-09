package com.zaneschepke.wireguardautotunnel.ui.screens.settings.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ForwardButton
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem

@Composable
fun proxYSettingsItem(onClick: () -> Unit): SelectionItem {
    return SelectionItem(
        leading = { Icon(ImageVector.vectorResource(R.drawable.proxy), null) },
        trailing = { ForwardButton { onClick() } },
        title = {
            Text(
                text = stringResource(R.string.proxy_settings),
                style =
                    MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
            )
        },
        onClick = onClick,
    )
}
