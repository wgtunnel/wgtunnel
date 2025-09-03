package com.zaneschepke.wireguardautotunnel.util.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

fun <K, V> Flow<Map<K, V>>.distinctByKeys(): Flow<Map<K, V>> {
    return distinctUntilChanged { old, new -> old.keys == new.keys }
}
