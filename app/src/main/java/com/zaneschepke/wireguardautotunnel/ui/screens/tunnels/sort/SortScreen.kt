package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.sort

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
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.LocalIsAndroidTV
import com.zaneschepke.wireguardautotunnel.ui.LocalSharedVm
import com.zaneschepke.wireguardautotunnel.ui.common.ExpandingRowListItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.ActionIconButton
import com.zaneschepke.wireguardautotunnel.ui.state.NavbarState
import com.zaneschepke.wireguardautotunnel.util.extensions.isSortedBy
import com.zaneschepke.wireguardautotunnel.viewmodel.TunnelsViewModel
import sh.calvin.reorderable.DragGestureDetector
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun SortScreen(viewModel: TunnelsViewModel) {
    val sharedViewModel = LocalSharedVm.current
    val tunnelsState by viewModel.container.stateFlow.collectAsStateWithLifecycle()
    val hapticFeedback = LocalHapticFeedback.current
    val isTv = LocalIsAndroidTV.current

    var sortAscending by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var editableTunnels by rememberSaveable { mutableStateOf(tunnelsState.tunnels) }

    LaunchedEffect(Unit) {
        sharedViewModel.updateNavbarState(
            NavbarState(
                showBottomItems = true,
                topTitle = { Text(stringResource(R.string.sort)) },
                topTrailing = {
                    Row {
                        ActionIconButton(Icons.Rounded.SortByAlpha, R.string.sort) {
                            sortAscending =
                                when (sortAscending) {
                                    null -> !editableTunnels.isSortedBy { it.name }
                                    true -> false
                                    false -> null
                                }
                            editableTunnels =
                                when (sortAscending) {
                                    true -> editableTunnels.sortedBy { it.name }
                                    false -> editableTunnels.sortedByDescending { it.name }
                                    null -> tunnelsState.tunnels
                                }
                        }
                        ActionIconButton(Icons.Rounded.Save, R.string.save) {
                            viewModel.saveSortChanges(editableTunnels)
                        }
                    }
                },
            )
        )
    }

    val lazyListState = rememberLazyListState()

    val reorderableLazyListState =
        rememberReorderableLazyListState(
            lazyListState,
            scrollThresholdPadding = WindowInsets.systemBars.asPaddingValues(),
        ) { from, to ->
            editableTunnels =
                editableTunnels.toMutableList().apply { add(to.index, removeAt(from.index)) }
            hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }

    LazyColumn(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.Top),
        modifier =
            Modifier.pointerInput(Unit) { if (tunnelsState.tunnels.isEmpty()) return@pointerInput }
                .overscroll(rememberOverscrollEffect())
                .padding(horizontal = 16.dp, vertical = 24.dp),
        state = lazyListState,
        userScrollEnabled = true,
        reverseLayout = false,
        flingBehavior = ScrollableDefaults.flingBehavior(),
    ) {
        itemsIndexed(editableTunnels, key = { _, tunnel -> tunnel.id }) { index, tunnel ->
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
                                        editableTunnels =
                                            editableTunnels.toMutableList().apply {
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
                                        editableTunnels =
                                            editableTunnels.toMutableList().apply {
                                                add(index + 1, removeAt(index))
                                            }
                                    },
                                    enabled = index != editableTunnels.size - 1,
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
