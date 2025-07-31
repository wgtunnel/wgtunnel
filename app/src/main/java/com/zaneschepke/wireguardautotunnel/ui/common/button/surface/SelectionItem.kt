package com.zaneschepke.wireguardautotunnel.ui.common.button.surface

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class SelectionItem(
    val leading: (@Composable () -> Unit)? = null,
    val trailing: (@Composable () -> Unit)? = null,
    val title: (@Composable () -> Unit),
    val description: (@Composable () -> Unit)? = null,
    val onClick: (() -> Unit)? = null,
    val modifier: Modifier = Modifier.height(64.dp),
)
