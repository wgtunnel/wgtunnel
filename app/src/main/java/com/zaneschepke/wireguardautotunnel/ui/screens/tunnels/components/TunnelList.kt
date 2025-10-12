package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState
import com.zaneschepke.wireguardautotunnel.ui.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.ui.common.button.SwitchWithDivider
import com.zaneschepke.wireguardautotunnel.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.ui.state.TunnelsUiState
import com.zaneschepke.wireguardautotunnel.util.extensions.asColor
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
) {
    val navController = LocalNavController.current
    val context = LocalContext.current

    val lazyListState = rememberLazyListState()

    LazyColumn(
        horizontalAlignment = Alignment.Start,
        modifier =
            modifier
                .pointerInput(Unit) {
                    detectTapGestures {
                        if (tunnelsState.tunnels.isEmpty()) return@detectTapGestures
                        viewModel.clearSelectedTunnels()
                    }
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
            var leadingIconColor by
                remember(
                    tunnelState.status,
                    tunnelState.logHealthState,
                    tunnelState.pingStates,
                    tunnelState.statistics,
                ) {
                    mutableStateOf(tunnelState.health().asColor())
                }

            SurfaceRow(
                leading = {
                    Icon(
                        Icons.Rounded.Circle,
                        contentDescription = stringResource(R.string.tunnel_monitoring),
                        tint = leadingIconColor,
                        modifier = Modifier.size(14.dp),
                    )
                },
                title = tunnel.tunName,
                onClick = {
                    if (tunnelsState.selectedTunnels.isNotEmpty()) {
                        viewModel.toggleSelectedTunnel(tunnel.id)
                    } else {
                        navController.push(Route.TunnelOptions(tunnel.id))
                        viewModel.clearSelectedTunnels()
                    }
                },
                selected = selected,
                expandedContent =
                    if (!tunnelState.status.isDown()) {
                        {
                            TunnelStatisticsRow(
                                tunnelState,
                                tunnelsState.isPingEnabled,
                                tunnelsState.showPingStats,
                            )
                        }
                    } else null,
                onLongClick = { viewModel.toggleSelectedTunnel(tunnel.id) },
                trailing = { modifier ->
                    SwitchWithDivider(
                        checked = tunnelState.status.isUpOrStarting(),
                        onClick = { checked ->
                            if (checked) sharedViewModel.startTunnel(tunnel)
                            else sharedViewModel.stopTunnel(tunnel)
                        },
                        modifier = modifier,
                    )
                },
            )
        }
    }
}
