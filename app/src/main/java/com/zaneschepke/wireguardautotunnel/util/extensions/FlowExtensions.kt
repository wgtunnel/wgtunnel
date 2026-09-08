package com.zaneschepke.wireguardautotunnel.util.extensions

import kotlin.time.Duration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest

/**
 * Passes `true` through immediately, but only passes `false` through after the upstream has held
 * `false` continuously for [timeout] with no intervening `true`.
 */
fun Flow<Boolean>.debounceFalling(timeout: Duration): Flow<Boolean> = channelFlow {
    collectLatest { value ->
        if (value) {
            send(true)
        } else {
            delay(timeout)
            send(false)
        }
    }
}
