package com.zaneschepke.wireguardautotunnel.ui.common.snackbar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.ui.navigation.LocalIsAndroidTV

@Composable
fun CustomSnackBar(
    message: String,
    isRtl: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surface,
) {
    val isTv = LocalIsAndroidTV.current
    Snackbar(
        containerColor = containerColor,
        modifier = Modifier.fillMaxWidth(if (isTv) 1 / 3f else 2 / 3f).padding(bottom = 100.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
        ) {
            Row(
                modifier = Modifier.width(IntrinsicSize.Max).height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                val icon = Icons.Rounded.Info
                Icon(
                    icon,
                    contentDescription = icon.name,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(end = 10.dp),
                )
                Text(
                    message,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(end = 5.dp),
                )
            }
        }
    }
}
