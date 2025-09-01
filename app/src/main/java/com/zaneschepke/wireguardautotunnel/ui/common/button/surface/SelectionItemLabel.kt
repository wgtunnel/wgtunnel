package com.zaneschepke.wireguardautotunnel.ui.common.button.surface

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SelectionItemLabel(text: String, labelType: SelectionLabelType, modifier: Modifier = Modifier) {

    val style =
        when (labelType) {
            SelectionLabelType.DESCRIPTION ->
                MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline)
            SelectionLabelType.TITLE ->
                MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                )
        }

    Text(text = text, style = style, modifier = modifier)
}

enum class SelectionLabelType {

    DESCRIPTION,
    TITLE,
}
