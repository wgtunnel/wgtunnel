package com.zaneschepke.wireguardautotunnel.ui.screens.main.sort

import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.ExpandingRowListItem
import com.zaneschepke.wireguardautotunnel.ui.navigation.LocalIsAndroidTV
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.util.extensions.isSortedBy
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent
import com.zaneschepke.wireguardautotunnel.viewmodel.event.UiEvent
import sh.calvin.reorderable.DragGestureDetector
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun SortScreen(appUiState: AppUiState, viewModel: AppViewModel) {

    val hapticFeedback = LocalHapticFeedback.current
    val isTv = LocalIsAndroidTV.current

    var sortAscending by remember { mutableStateOf<Boolean?>(null) }
    var sortedTunnels by remember { mutableStateOf(appUiState.tunnels.sortedBy { it.position }) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { uiEvent ->
            when (uiEvent) {
                UiEvent.SortTunnels -> {
                    sortAscending =
                        when (sortAscending) {
                            null -> !sortedTunnels.isSortedBy { it.name }
                            true -> false
                            false -> null
                        }
                    sortedTunnels =
                        when (sortAscending) {
                            true -> sortedTunnels.sortedBy { it.name }
                            false -> sortedTunnels.sortedByDescending { it.name }
                            null -> sortedTunnels.sortedBy { it.position }
                        }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.handleEvent(
            AppEvent.SetScreenAction {
                viewModel.handleEvent(
                    AppEvent.SaveAllConfigs(
                        sortedTunnels.mapIndexed { index, conf -> conf.copy(position = index) }
                    )
                )
                viewModel.handleEvent(AppEvent.PopBackStack(true))
            }
        )
    }

    val lazyListState = rememberLazyListState()

    val reorderableLazyListState =
        rememberReorderableLazyListState(
            lazyListState,
            scrollThresholdPadding = WindowInsets.systemBars.asPaddingValues(),
        ) { from, to ->
            sortedTunnels =
                sortedTunnels.toMutableList().apply { add(to.index, removeAt(from.index)) }
            hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }

    LazyColumn(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.Top),
        modifier =
            Modifier.pointerInput(Unit) { if (appUiState.tunnels.isEmpty()) return@pointerInput }
                .overscroll(rememberOverscrollEffect())
                .padding(horizontal = 16.dp, vertical = 24.dp),
        state = lazyListState,
        userScrollEnabled = true,
        reverseLayout = false,
        flingBehavior = ScrollableDefaults.flingBehavior(),
    ) {
        itemsIndexed(sortedTunnels, key = { _, tunnel -> tunnel.id }) { index, tunnel ->
            ReorderableItem(reorderableLazyListState, tunnel.id) { isDragging ->
                ExpandingRowListItem(
                    leading = {},
                    text = tunnel.name,
                    trailing = {
                        if (!isTv)
                            Icon(
                                Icons.Default.DragHandle,
                                stringResource(
                                    com.zaneschepke.wireguardautotunnel.R.string.drag_handle
                                ),
                            )
                        else
                            Row {
                                IconButton(
                                    onClick = {
                                        sortedTunnels =
                                            sortedTunnels.toMutableList().apply {
                                                add(index - 1, removeAt(index))
                                            }
                                    },
                                    enabled = index != 0,
                                ) {
                                    Icon(
                                        Icons.Default.ArrowUpward,
                                        stringResource(
                                            com.zaneschepke.wireguardautotunnel.R.string.move_up
                                        ),
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        sortedTunnels =
                                            sortedTunnels.toMutableList().apply {
                                                add(index + 1, removeAt(index))
                                            }
                                    },
                                    enabled = index != sortedTunnels.count() - 1,
                                ) {
                                    Icon(
                                        Icons.Default.ArrowDownward,
                                        stringResource(R.string.move_down),
                                    )
                                }
                            }
                    },
                    isSelected = isDragging,
                    expanded = {},
                    modifier =
                        Modifier.draggableHandle(
                            onDragStarted = {
                                hapticFeedback.performHapticFeedback(
                                    HapticFeedbackType.GestureThresholdActivate
                                )
                            },
                            onDragStopped = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)
                            },
                            dragGestureDetector = DragGestureDetector.LongPress,
                        ),
                )
            }
        }
    }
}
