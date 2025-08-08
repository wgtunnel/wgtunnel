package com.zaneschepke.wireguardautotunnel.ui.screens.main.config.state

import com.zaneschepke.wireguardautotunnel.ui.state.ConfigProxy
import com.zaneschepke.wireguardautotunnel.ui.state.InterfaceProxy
import com.zaneschepke.wireguardautotunnel.ui.state.PeerProxy

data class ConfigUiState(
    val tunnelName: String = "",
    val configProxy: ConfigProxy =
        ConfigProxy(`interface` = InterfaceProxy(), peers = listOf(PeerProxy())),
    val showAmneziaValues: Boolean = false,
    val showScripts: Boolean = false,
    val isAuthenticated: Boolean = true,
    val showAuthPrompt: Boolean = false,
    val saveChanges: Boolean = false,
)
