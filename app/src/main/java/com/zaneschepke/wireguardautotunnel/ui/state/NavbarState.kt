package com.zaneschepke.wireguardautotunnel.ui.state

import androidx.compose.runtime.Composable

data class NavbarState(
    val topTitle: String? = null,
    val topTrailing: (@Composable () -> Unit)? = null,
    val topLeading: (@Composable () -> Unit)? = null,
    val showBottomItems: Boolean = false,
    val removeBottom: Boolean = false,
    val isAutoTunnelActive: Boolean = false,
)
