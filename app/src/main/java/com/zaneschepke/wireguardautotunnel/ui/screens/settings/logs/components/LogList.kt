package com.zaneschepke.wireguardautotunnel.ui.screens.settings.logs.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zaneschepke.logcatter.model.LogMessage

@Composable
fun LogList(
    logs: List<LogMessage>,
    lazyColumnListState: LazyListState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
        state = lazyColumnListState,
        modifier = modifier.padding(horizontal = 16.dp),
    ) {
        itemsIndexed(items = logs, key = { index, _ -> index }, contentType = { _, _ -> null }) {
            _,
            log ->
            LogItem(log = log)
        }
    }
}
