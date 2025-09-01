package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R

@Composable
fun GettingStartedLabel(onClick: (url: String) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(top = 100.dp).fillMaxSize(),
    ) {
        val gettingStarted = buildAnnotatedString {
            append(stringResource(id = R.string.see_the))
            append(" ")
            pushStringAnnotation(
                tag = "gettingStarted",
                annotation = stringResource(id = R.string.getting_started_url),
            )
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                append(stringResource(id = R.string.getting_started_guide))
            }
            pop()
            append(" ")
            append(stringResource(R.string.unsure_how))
            append(".")
        }
        Text(text = stringResource(R.string.no_tunnels), fontStyle = FontStyle.Italic)
        ClickableText(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 24.dp),
            text = gettingStarted,
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                ),
        ) {
            gettingStarted
                .getStringAnnotations(tag = "gettingStarted", it, it)
                .firstOrNull()
                ?.let { annotation -> onClick(annotation.item) }
        }
    }
}
