package com.zaneschepke.wireguardautotunnel.ui.common.label

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.util.*

@Composable
fun lowercaseLabel(text: String): String {
    val locale = remember { Locale.getDefault() }
    return text.lowercase(locale)
}
