package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.tunneloptions.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem

@Composable
fun preferIpv6Item(checked: Boolean, onClick: () -> Unit): SelectionItem {
    return SelectionItem(
        leading = { Icon(ImageVector.vectorResource(R.drawable.host), contentDescription = null) },
        title = {
            Text(
                text = stringResource(R.string.prefer_ipv6_resolution),
                style =
                    MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
            )
        },
        trailing = { ScaledSwitch(checked = checked, onClick = { onClick() }) },
        onClick = onClick,
    )
}
