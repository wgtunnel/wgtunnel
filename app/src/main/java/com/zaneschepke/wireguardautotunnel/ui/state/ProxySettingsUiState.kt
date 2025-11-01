package com.zaneschepke.wireguardautotunnel.ui.state

import com.zaneschepke.wireguardautotunnel.domain.model.ProxySettings
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState

data class ProxySettingsUiState(
    val proxySettings: ProxySettings = ProxySettings(),
    val activeTuns: Map<Int, TunnelState> = emptyMap(),
    val isSocks5BindAddressError: Boolean = false,
    val isHttpBindAddressError: Boolean = false,
    val isUserNameError: Boolean = false,
    val isPasswordError: Boolean = false,
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = true,
    val showSaveModal: Boolean = false,
)
