package com.zaneschepke.wireguardautotunnel.ui.state

import com.zaneschepke.wireguardautotunnel.domain.model.DnsSettings
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConfig

data class DnsUiState(
    val isLoading: Boolean = true,
    val dnsSettings: DnsSettings = DnsSettings(),
    val globalConfig: TunnelConfig? = null,
)
