package com.zaneschepke.wireguardautotunnel.ui.screens.main.splittunnel.components

import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.zaneschepke.wireguardautotunnel.ui.common.button.SelectionItemButton
import com.zaneschepke.wireguardautotunnel.ui.screens.main.splittunnel.state.TunnelApp
import com.zaneschepke.wireguardautotunnel.ui.theme.iconSize

@Composable
fun AppListItem(appInfo: TunnelApp, isSelected: Boolean, onToggle: () -> Unit) {
    val context = LocalContext.current
    val icon =
        remember(appInfo.`package`) {
            try {
                context.packageManager.getApplicationIcon(appInfo.`package`)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
        }

    SelectionItemButton(
        leading = {
            Image(
                painter = rememberDrawablePainter(icon),
                contentDescription = appInfo.name,
                modifier = Modifier.padding(horizontal = 24.dp).size(iconSize),
            )
        },
        buttonText = appInfo.name,
        onClick = onToggle,
        trailing = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = isSelected, onCheckedChange = { onToggle() })
            }
        },
    )
}
