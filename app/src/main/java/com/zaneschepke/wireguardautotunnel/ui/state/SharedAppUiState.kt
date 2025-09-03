package com.zaneschepke.wireguardautotunnel.ui.state

import androidx.compose.runtime.Composable
import com.zaneschepke.wireguardautotunnel.domain.model.GeneralSettings
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.ui.theme.Theme
import com.zaneschepke.wireguardautotunnel.util.LocaleUtil

data class SharedAppUiState(
    val isAppLoaded: Boolean = false,
    val theme: Theme = Theme.AUTOMATIC,
    val locale: String = LocaleUtil.OPTION_PHONE_LANGUAGE,
    val pinLockEnabled: Boolean = false,
    val isAuthorized: Boolean = false,
    val isAutoTunnelActive: Boolean = false,
    val isLocationDisclosureShown: Boolean = false,
    val tunnels: List<TunnelConf> = emptyList(),
    val settings: GeneralSettings = GeneralSettings(),
    val topNavActions: (@Composable () -> Unit)? = null,
)
