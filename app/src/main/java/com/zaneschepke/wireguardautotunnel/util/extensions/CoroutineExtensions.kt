package com.zaneschepke.wireguardautotunnel.util.extensions

import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import kotlinx.coroutines.flow.*

fun <K, V> Flow<Map<K, V>>.distinctByKeys(): Flow<Map<K, V>> {
    return distinctUntilChanged { old, new -> old.keys == new.keys }
}

fun <T> Flow<T>.zipWithPrevious(): Flow<Pair<T?, T>> = flow {
    var previous: T? = null
    collect { current ->
        emit(previous to current)
        previous = current
    }
}

suspend fun <R> StateFlow<AppUiState>.withFirstState(block: suspend (AppUiState) -> R): R {
    return block(first { it.isAppLoaded })
}
