package com.zaneschepke.wireguardautotunnel.ui.state

import com.zaneschepke.logcatter.model.LogMessage

data class LoggerUiState(val messages: List<LogMessage> = emptyList())
