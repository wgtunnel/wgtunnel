package com.zaneschepke.wireguardautotunnel.ui.common.button

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zaneschepke.wireguardautotunnel.ui.theme.iconSize

@Composable
fun LaunchButton(modifier: Modifier = Modifier.focusable(), onClick: () -> Unit) {
    IconButton(modifier = modifier, onClick = onClick) {
        val icon = Icons.AutoMirrored.Outlined.Launch
        Icon(icon, icon.name, Modifier.size(iconSize))
    }
}
