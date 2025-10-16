package com.zaneschepke.wireguardautotunnel.ui.state

import com.zaneschepke.wireguardautotunnel.domain.model.DnsSettings

data class DnsUiState(val isLoading: Boolean = true, val dnsSettings: DnsSettings = DnsSettings())
