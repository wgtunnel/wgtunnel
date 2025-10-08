package com.zaneschepke.wireguardautotunnel.ui.state

import com.zaneschepke.wireguardautotunnel.domain.model.GeneralSettings
import com.zaneschepke.wireguardautotunnel.ui.theme.Theme
import com.zaneschepke.wireguardautotunnel.util.LocaleUtil

data class SharedAppUiState(
    val isAppLoaded: Boolean = false,
    val theme: Theme = Theme.AUTOMATIC,
    val locale: String = LocaleUtil.OPTION_PHONE_LANGUAGE,
    val pinLockEnabled: Boolean = false,
    val tunnelNames: Map<Int, String> = emptyMap(),
    val selectedTunnelCount: Int = 0,
    val isAuthorized: Boolean = false,
    val isAutoTunnelActive: Boolean = false,
    val isLocationDisclosureShown: Boolean = false,
    val settings: GeneralSettings = GeneralSettings(),
)
