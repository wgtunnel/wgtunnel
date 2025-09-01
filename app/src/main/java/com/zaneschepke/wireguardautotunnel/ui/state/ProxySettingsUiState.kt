package com.zaneschepke.wireguardautotunnel.ui.state

import com.zaneschepke.wireguardautotunnel.domain.model.AppProxySettings

data class ProxySettingsUiState(
    val proxySettings: AppProxySettings = AppProxySettings(),
    val isSocks5BindAddressError: Boolean = false,
    val isHttpBindAddressError: Boolean = false,
    val isUserNameError: Boolean = false,
    val isPasswordError: Boolean = false,
    val passwordVisible: Boolean = false,
    val stateInitialized: Boolean = false,
)
