package com.zaneschepke.wireguardautotunnel.ui.common.button

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun ClickableIconButton(
    onClick: () -> Unit,
    onIconClick: () -> Unit,
    text: String,
    icon: ImageVector,
    enabled: Boolean = true,
) {
    TextButton(onClick = onClick, enabled = enabled) {
        Text(
            text,
            Modifier.weight(1f, false),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
        Icon(
            imageVector = icon,
            contentDescription = icon.name,
            modifier =
                Modifier.size(ButtonDefaults.IconSize).weight(1f, false).clickable {
                    if (enabled) {
                        onIconClick()
                    }
                },
        )
    }
}
