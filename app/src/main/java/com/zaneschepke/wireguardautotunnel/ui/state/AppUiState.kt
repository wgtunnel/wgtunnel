package com.zaneschepke.wireguardautotunnel.ui.state

import com.zaneschepke.networkmonitor.ConnectivityState
import com.zaneschepke.wireguardautotunnel.data.entity.GeneralState
import com.zaneschepke.wireguardautotunnel.data.mapper.GeneralStateMapper
import com.zaneschepke.wireguardautotunnel.domain.model.AppProxySettings
import com.zaneschepke.wireguardautotunnel.domain.model.AppSettings
import com.zaneschepke.wireguardautotunnel.domain.model.AppState
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState

data class AppUiState(
    val appSettings: AppSettings = AppSettings(),
    val proxySettings: AppProxySettings = AppProxySettings(),
    val tunnels: List<TunnelConf> = emptyList(),
    val activeTunnels: Map<TunnelConf, TunnelState> = emptyMap(),
    val appState: AppState = GeneralStateMapper.toAppState(GeneralState()),
    val isAutoTunnelActive: Boolean = false,
    val appConfigurationChange: Boolean = false,
    val isAppLoaded: Boolean = false,
    val connectivityState: ConnectivityState? = null,
)
