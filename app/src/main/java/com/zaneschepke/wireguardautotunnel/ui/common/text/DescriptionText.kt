package com.zaneschepke.wireguardautotunnel.ui.common.text

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import com.zaneschepke.wireguardautotunnel.ui.theme.Disabled

@Composable
fun DescriptionText(text: String, modifier: Modifier = Modifier, disabled: Boolean = false) {
    Text(
        text = text,
        style =
            MaterialTheme.typography.bodySmall.copy(
                color = if (disabled) Disabled else MaterialTheme.colorScheme.outline
            ),
        modifier = modifier,
    )
}

@Composable
fun DescriptionText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    disabled: Boolean = false,
) {
    Text(
        text = text,
        style =
            MaterialTheme.typography.bodySmall.copy(
                color = if (disabled) Disabled else MaterialTheme.colorScheme.outline
            ),
        modifier = modifier,
    )
}
