package com.zaneschepke.wireguardautotunnel.util.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

fun <K, V> Flow<Map<K, V>>.distinctByKeys(): Flow<Map<K, V>> {
    return distinctUntilChanged { old, new -> old.keys == new.keys }
}

fun <T> debounce(
    scope: CoroutineScope,
    delayMillis: Long = 300L,
    onDebounced: (T) -> Unit,
): (T) -> Unit {
    var job: Job? = null
    return { param: T ->
        job?.cancel()
        job =
            scope.launch {
                delay(delayMillis)
                onDebounced(param)
            }
    }
}
