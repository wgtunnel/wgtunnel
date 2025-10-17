package com.zaneschepke.wireguardautotunnel.domain.events

import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.util.StringValue

sealed class BackendMessage {

    data object DynamicDnsSuccess : BackendMessage()

    fun toStringRes() =
        when (this) {
            DynamicDnsSuccess -> R.string.ddns_success_message
        }

    fun toStringValue() = StringValue.StringResource(this.toStringRes())
}
