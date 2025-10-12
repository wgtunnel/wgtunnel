package com.zaneschepke.wireguardautotunnel.ui.common.text

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun DescriptionText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline),
        modifier = modifier,
    )
}
