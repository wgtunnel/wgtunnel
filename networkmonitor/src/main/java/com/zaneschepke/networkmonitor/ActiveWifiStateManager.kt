package com.zaneschepke.networkmonitor

import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

// keep track of the currently active network(s)
class ActiveWifiStateManager {
    private val _stateFlow =
        MutableStateFlow(linkedMapOf<String, Pair<Network?, NetworkCapabilities?>>())

    @Synchronized
    fun put(key: String, value: Pair<Network?, NetworkCapabilities?>) {
        _stateFlow.update { currentMap ->
            linkedMapOf(*currentMap.toList().toTypedArray()).apply { put(key, value) }
        }
    }

    @Synchronized
    fun remove(key: String) {
        _stateFlow.update { currentMap ->
            linkedMapOf(*currentMap.toList().toTypedArray()).apply { remove(key) }
        }
    }

    fun isEmpty(): Boolean = _stateFlow.value.isEmpty()

    fun getLatestValue(): Pair<Network?, NetworkCapabilities?>? {
        return _stateFlow.value.entries.lastOrNull()?.value
    }

    @Synchronized
    fun clear() {
        _stateFlow.update { currentMap ->
            linkedMapOf(*currentMap.toList().toTypedArray()).apply { clear() }
        }
    }
}
