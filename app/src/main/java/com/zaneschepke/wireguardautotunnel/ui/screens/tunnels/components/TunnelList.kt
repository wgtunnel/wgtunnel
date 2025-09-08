package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState
import com.zaneschepke.wireguardautotunnel.ui.LocalIsAndroidTV
import com.zaneschepke.wireguardautotunnel.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.ui.state.TunnelsUiState
import com.zaneschepke.wireguardautotunnel.util.extensions.openWebUrl
import com.zaneschepke.wireguardautotunnel.viewmodel.SharedAppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.TunnelsViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TunnelList(
    tunnelsState: TunnelsUiState,
    modifier: Modifier = Modifier,
    viewModel: TunnelsViewModel,
    sharedViewModel: SharedAppViewModel,
    navController: NavController,
) {
    val isTv = LocalIsAndroidTV.current
    val context = LocalContext.current

    val lazyListState = rememberLazyListState()

    LazyColumn(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.Top),
        modifier =
            modifier
                .pointerInput(Unit) {
                    if (tunnelsState.tunnels.isEmpty()) return@pointerInput
                    viewModel.clearSelectedTunnels()
                }
                .overscroll(rememberOverscrollEffect()),
        state = lazyListState,
        userScrollEnabled = true,
        reverseLayout = false,
        flingBehavior = ScrollableDefaults.flingBehavior(),
    ) {
        if (tunnelsState.tunnels.isEmpty()) {
            item { GettingStartedLabel(onClick = { context.openWebUrl(it) }) }
        }
        items(tunnelsState.tunnels, key = { it.id }) { tunnel ->
            val tunnelState =
                remember(tunnelsState.activeTunnels) {
                    tunnelsState.activeTunnels[tunnel.id] ?: TunnelState()
                }
            val selected =
                remember(tunnelsState.selectedTunnels) {
                    tunnelsState.selectedTunnels.any { it.id == tunnel.id }
                }
            TunnelRowItem(
                state = tunnelState,
                isSelected = selected,
                tunnel = tunnel,
                tunnelState = tunnelState,
                onTvClick = { navController.navigate(Route.TunnelOptions(tunnel.id)) },
                onToggleSelectedTunnel = { tunnel -> viewModel.toggleSelectedTunnel(tunnel.id) },
                onSwitchClick = { checked ->
                    if (checked) sharedViewModel.startTunnel(tunnel)
                    else sharedViewModel.stopTunnel(tunnel)
                },
                isTv = isTv,
                isPingEnabled = tunnelsState.isPingEnabled,
                showDetailedStats = tunnelsState.showPingStats,
                modifier =
                    if (!isTv)
                        Modifier.combinedClickable(
                            onClick = {
                                if (tunnelsState.selectedTunnels.isNotEmpty()) {
                                    viewModel.toggleSelectedTunnel(tunnel.id)
                                } else {
                                    navController.navigate(Route.TunnelOptions(tunnel.id))
                                    viewModel.clearSelectedTunnels()
                                }
                            },
                            onLongClick = { viewModel.toggleSelectedTunnel(tunnel.id) },
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        )
                    else Modifier,
            )
        }
    }
}
