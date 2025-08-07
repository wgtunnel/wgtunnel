package com.zaneschepke.wireguardautotunnel.ui.screens.settings.logs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.logs.components.LogList
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.logs.components.LogsBottomSheet
import com.zaneschepke.wireguardautotunnel.ui.state.AppViewState
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel

@Composable
fun LogsScreen(appViewState: AppViewState, viewModel: AppViewModel) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()

    val lazyColumnListState = rememberLazyListState()
    var isAutoScrolling by remember { mutableStateOf(true) }
    var lastScrollPosition by remember { mutableIntStateOf(0) }

    LaunchedEffect(isAutoScrolling) {
        if (isAutoScrolling) {
            lazyColumnListState.animateScrollToItem(logs.size)
        }
    }

    LaunchedEffect(logs.size) {
        if (isAutoScrolling) {
            lazyColumnListState.animateScrollToItem(logs.size)
        }
    }

    LaunchedEffect(lazyColumnListState) {
        snapshotFlow { lazyColumnListState.firstVisibleItemIndex }
            .collect { currentScrollPosition ->
                if (currentScrollPosition < lastScrollPosition && isAutoScrolling) {
                    isAutoScrolling = false
                }
                val visible = lazyColumnListState.layoutInfo.visibleItemsInfo
                if (
                    visible.isNotEmpty() &&
                        visible.last().index ==
                            lazyColumnListState.layoutInfo.totalItemsCount - 1 &&
                        !isAutoScrolling
                ) {
                    isAutoScrolling = true
                }
                lastScrollPosition = currentScrollPosition
            }
    }

    when (appViewState.bottomSheet) {
        AppViewState.BottomSheet.LOGS -> {
            LogsBottomSheet(viewModel)
        }
        else -> Unit
    }

    if (logs.isEmpty()) {
        return Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.nothing_here_yet),
                fontStyle = FontStyle.Italic,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }

    LogList(
        logs = logs,
        lazyColumnListState = lazyColumnListState,
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
    )
}
