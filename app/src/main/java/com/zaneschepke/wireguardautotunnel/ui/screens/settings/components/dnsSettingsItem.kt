package com.zaneschepke.wireguardautotunnel.ui.screens.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.ui.common.button.ForwardButton
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.util.extensions.asString

@Composable
fun dnsSettingsItem(
    appMode: AppMode,
    onClick: () -> Unit,
    onDisabledClick: () -> Unit,
): SelectionItem {
    val context = LocalContext.current
    val enabled by rememberSaveable(appMode) { mutableStateOf(appMode != AppMode.KERNEL) }
    val click = if (enabled) onClick else onDisabledClick
    return SelectionItem(
        leading = { Icon(Icons.Outlined.Dns, null) },
        trailing = { ForwardButton { click() } },
        title = {
            Text(
                text = stringResource(R.string.dns_settings),
                style =
                    MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
            )
        },
        onClick = click,
        disabledReason =
            context.getString(R.string.mode_disabled_template, appMode.asString(context)),
    )
}
