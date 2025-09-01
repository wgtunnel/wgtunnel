package com.zaneschepke.wireguardautotunnel.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val name: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val active: Boolean = false,
    val route: Route,
)
