package com.zaneschepke.wireguardautotunnel.ui.common.button

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.ui.theme.iconSize

@Composable
fun ActionIconButton(icon: ImageVector, labelRes: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            icon,
            contentDescription = stringResource(labelRes),
            modifier = Modifier.size(iconSize),
        )
    }
}
