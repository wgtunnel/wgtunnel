package com.zaneschepke.wireguardautotunnel.ui.common.snackbar

import androidx.compose.ui.text.AnnotatedString

enum class SnackbarType {
    INFO,
    WARNING,
    THANK_YOU,
}

data class SnackbarInfo(
    val message: AnnotatedString,
    val type: SnackbarType = SnackbarType.INFO,
    val durationMs: Long = 4000L,
    val id: String = System.currentTimeMillis().toString(),
)
