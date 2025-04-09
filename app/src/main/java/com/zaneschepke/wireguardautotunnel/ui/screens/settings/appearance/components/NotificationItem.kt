package com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ForwardButton
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.util.extensions.launchNotificationSettings

@Composable
fun NotificationsItem(): SelectionItem {
    val context = LocalContext.current
    return SelectionItem(
        leadingIcon = Icons.Outlined.Notifications,
        title = {
            Text(
                text = stringResource(R.string.notifications),
                style =
                    MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
            )
        },
        trailing = { ForwardButton { context.launchNotificationSettings() } },
        onClick = { context.launchNotificationSettings() },
    )
}
