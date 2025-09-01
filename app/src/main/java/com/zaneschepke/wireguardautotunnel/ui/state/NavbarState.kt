package com.zaneschepke.wireguardautotunnel.ui.state

import androidx.compose.runtime.Composable

data class NavbarState(
    val topTitle: (@Composable () -> Unit)? = null,
    val topTrailing: (@Composable () -> Unit)? = null,
    val showTopItems: Boolean = false,
    val showBottomItems: Boolean = false,
    val removeBottom: Boolean = false,
    val removeTop: Boolean = false,
    val locationDisclosureShown: Boolean = false,
    val isAutoTunnelActive: Boolean = false,
)
