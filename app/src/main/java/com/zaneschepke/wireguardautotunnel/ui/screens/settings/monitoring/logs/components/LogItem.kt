package com.zaneschepke.wireguardautotunnel.ui.screens.settings.monitoring.logs.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaneschepke.logcatter.model.LogMessage
import com.zaneschepke.wireguardautotunnel.ui.common.functions.rememberClipboardHelper
import com.zaneschepke.wireguardautotunnel.ui.common.text.LogTypeLabel

@Composable
fun LogItem(log: LogMessage) {
    val clipboardManager = rememberClipboardHelper()
    val fontSize = 10.sp

    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.Start),
        verticalAlignment = Alignment.Top,
        modifier =
            Modifier.fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { clipboardManager.copy(log.toString()) },
                ),
    ) {
        Text(text = log.tag, modifier = Modifier.fillMaxSize(0.3f), fontSize = fontSize)
        LogTypeLabel(color = Color(log.level.color())) {
            Text(text = log.level.signifier, textAlign = TextAlign.Center, fontSize = fontSize)
        }
        Text(text = "${log.message} - ${log.time}", fontSize = fontSize)
    }
}
