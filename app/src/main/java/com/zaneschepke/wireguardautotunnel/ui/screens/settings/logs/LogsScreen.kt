package com.zaneschepke.wireguardautotunnel.ui.screens.settings.logs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.LocalSharedVm
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.logs.components.LogList
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.logs.components.LogsBottomSheet
import com.zaneschepke.wireguardautotunnel.ui.sideeffect.LocalSideEffect
import com.zaneschepke.wireguardautotunnel.viewmodel.LoggerViewModel
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun LogsScreen(viewModel: LoggerViewModel = hiltViewModel()) {
    val sharedAppViewModel = LocalSharedVm.current
    val loggerState by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    val lazyColumnListState = rememberLazyListState()
    var isAutoScrolling by rememberSaveable { mutableStateOf(true) }
    var lastScrollPosition by rememberSaveable { mutableIntStateOf(0) }
    var showLogsSheet by rememberSaveable { mutableStateOf(false) }

    sharedAppViewModel.collectSideEffect { sideEffect ->
        if (sideEffect is LocalSideEffect.Sheet.LoggerActions) showLogsSheet = true
    }

    LaunchedEffect(isAutoScrolling) {
        if (isAutoScrolling) {
            lazyColumnListState.animateScrollToItem(loggerState.messages.size)
        }
    }

    LaunchedEffect(loggerState.messages.size) {
        if (isAutoScrolling) {
            lazyColumnListState.animateScrollToItem(loggerState.messages.size)
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

    if (showLogsSheet) {
        LogsBottomSheet({ viewModel.exportLogs() }, { viewModel.deleteLogs() }) {
            showLogsSheet = false
        }
    }

    if (loggerState.messages.isEmpty()) {
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
        logs = loggerState.messages,
        lazyColumnListState = lazyColumnListState,
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
    )
}
