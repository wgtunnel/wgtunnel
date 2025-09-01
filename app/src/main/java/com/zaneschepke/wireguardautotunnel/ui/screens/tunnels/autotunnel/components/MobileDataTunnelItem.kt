package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.autotunnel.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem

@Composable
fun MobileDataTunnelItem(enabled: Boolean, onClick: (Boolean) -> Unit): SelectionItem {
    return SelectionItem(
        leading = { Icon(Icons.Outlined.PhoneAndroid, contentDescription = null) },
        title = {
            Text(
                text = stringResource(R.string.mobile_tunnel),
                style =
                    MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
            )
        },
        description = {
            Text(
                text = stringResource(R.string.mobile_data_tunnel),
                style = MaterialTheme.typography.bodySmall.copy(MaterialTheme.colorScheme.outline),
            )
        },
        trailing = { ScaledSwitch(checked = enabled, onClick = onClick) },
        onClick = { onClick(!enabled) },
    )
}
