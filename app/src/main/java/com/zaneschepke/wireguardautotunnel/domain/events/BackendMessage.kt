package com.zaneschepke.wireguardautotunnel.domain.events

import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.util.StringValue

sealed class BackendMessage {

    data object BounceSuccess : BackendMessage()

    data object BounceRecovery : BackendMessage()

    fun toStringRes() =
        when (this) {
            BounceRecovery -> R.string.pinger_bounce_recovery
            BounceSuccess -> R.string.pinger_bounce_successful
        }

    fun toStringValue() = StringValue.StringResource(this.toStringRes())
}
