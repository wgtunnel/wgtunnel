package com.zaneschepke.wireguardautotunnel.ui.screens.main.components

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
import com.zaneschepke.wireguardautotunnel.core.tunnel.getValueById
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.navigation.LocalIsAndroidTV
import com.zaneschepke.wireguardautotunnel.ui.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.util.extensions.openWebUrl
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TunnelList(
    appUiState: AppUiState,
    selectedTunnels: List<TunnelConf>,
    modifier: Modifier = Modifier,
    onToggleTunnel: (TunnelConf, Boolean) -> Unit,
    viewModel: AppViewModel,
) {
    val isTv = LocalIsAndroidTV.current
    val context = LocalContext.current
    val navController = LocalNavController.current

    val lazyListState = rememberLazyListState()

    LazyColumn(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.Top),
        modifier =
            modifier
                .pointerInput(Unit) { if (appUiState.tunnels.isEmpty()) return@pointerInput }
                .overscroll(rememberOverscrollEffect()),
        state = lazyListState,
        userScrollEnabled = true,
        reverseLayout = false,
        flingBehavior = ScrollableDefaults.flingBehavior(),
    ) {
        if (appUiState.tunnels.isEmpty()) {
            item { GettingStartedLabel(onClick = { context.openWebUrl(it) }) }
        }
        items(appUiState.tunnels, key = { it.id }) { tunnel ->
            val tunnelState =
                remember(appUiState.activeTunnels) {
                    appUiState.activeTunnels.getValueById(tunnel.id) ?: TunnelState()
                }
            val selected = remember(selectedTunnels) { selectedTunnels.any { it.id == tunnel.id } }
            TunnelRowItem(
                state = tunnelState,
                isSelected = selected,
                tunnel = tunnel,
                tunnelState = tunnelState,
                appSettings = appUiState.appSettings,
                onTvClick = {
                    navController.navigate(Route.TunnelOptions(tunnel.id))
                    viewModel.handleEvent(AppEvent.ClearSelectedTunnels)
                },
                onToggleSelectedTunnel = {
                    viewModel.handleEvent(AppEvent.ToggleSelectedTunnel(it))
                },
                onSwitchClick = { checked -> onToggleTunnel(tunnel, checked) },
                isTv = isTv,
                showDetailedStats = appUiState.appState.showDetailedPingStats,
                modifier =
                    if (!isTv)
                        Modifier.combinedClickable(
                            onClick = {
                                if (selectedTunnels.isNotEmpty()) {
                                    viewModel.handleEvent(AppEvent.ToggleSelectedTunnel(tunnel))
                                } else {
                                    navController.navigate(Route.TunnelOptions(tunnel.id))
                                    viewModel.handleEvent(AppEvent.ClearSelectedTunnels)
                                }
                            },
                            onLongClick = {
                                viewModel.handleEvent(AppEvent.ToggleSelectedTunnel(tunnel))
                            },
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        )
                    else Modifier,
            )
        }
    }
}
