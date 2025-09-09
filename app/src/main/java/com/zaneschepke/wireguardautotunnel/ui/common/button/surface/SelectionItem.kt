package com.zaneschepke.wireguardautotunnel.ui.common.button.surface

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class SelectionItem(
    val leading: (@Composable () -> Unit)? = null,
    val trailing: (@Composable () -> Unit)? = null,
    val title: (@Composable () -> Unit),
    val description: (@Composable () -> Unit)? = null,
    val onClick: (() -> Unit)? = null,
    val isEnabled: Boolean = true,
    val disabledReason: String? = null,
    val modifier: Modifier = Modifier.height(48.dp),
    val padding: Dp = if (description == null && disabledReason == null) 16.dp else 6.dp,
    val onLongPress: (() -> Unit)? = null,
)
