package com.zaneschepke.wireguardautotunnel.ui.navigation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.zaneschepke.wireguardautotunnel.ui.state.NavbarState
import com.zaneschepke.wireguardautotunnel.ui.theme.LockedDownBannerHeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicTopAppBar(navBarState: NavbarState, modifier: Modifier = Modifier) {
    TopAppBar(
        modifier = modifier.padding(top = LockedDownBannerHeight),
        colors = TopAppBarDefaults.topAppBarColors().copy(Color.Transparent),
        navigationIcon = { navBarState.topLeading?.invoke() },
        title = {
            navBarState.topTitle?.let { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        },
        actions = { navBarState.topTrailing?.invoke() },
    )
}
