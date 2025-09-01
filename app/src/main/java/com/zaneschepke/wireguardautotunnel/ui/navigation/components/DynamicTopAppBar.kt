package com.zaneschepke.wireguardautotunnel.ui.navigation.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.ui.state.NavbarState
import com.zaneschepke.wireguardautotunnel.ui.theme.LockedDownBannerHeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicTopAppBar(navBarState: NavbarState, modifier: Modifier = Modifier) {
    TopAppBar(
        modifier = modifier.padding(top = LockedDownBannerHeight),
        colors = TopAppBarDefaults.topAppBarColors().copy(Color.Transparent),
        title = {
            Box(modifier = Modifier.padding(start = 10.dp)) {
                AnimatedVisibility(
                    visible = navBarState.showTopItems,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut(),
                ) {
                    navBarState.topTitle?.invoke()
                }
            }
        },
        actions = {
            Box(modifier = Modifier.padding(end = 10.dp)) { navBarState.topTrailing?.invoke() }
        },
    )
}
