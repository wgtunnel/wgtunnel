package com.zaneschepke.wireguardautotunnel.ui.common.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicTopAppBar(navBarState: NavBarState, modifier: Modifier = Modifier) {
    TopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors().copy(Color.Transparent),
        title = {
            AnimatedVisibility(
                visible = navBarState.showTop,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
            ) {
                Box(modifier = Modifier.padding(start = 10.dp)) { navBarState.topTitle?.invoke() }
            }
        },
        actions = {
            AnimatedVisibility(
                visible = navBarState.showTop,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
            ) {
                Box(modifier = Modifier.padding(end = 10.dp)) { navBarState.topTrailing?.invoke() }
            }
        },
    )
}
