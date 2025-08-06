package com.zaneschepke.wireguardautotunnel.core.service.autotunnel

import com.zaneschepke.wireguardautotunnel.domain.model.AppSettings
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.state.NetworkState
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState
import com.zaneschepke.wireguardautotunnel.util.extensions.Tunnels

sealed class StateChange {
    data class NetworkChange(val networkState: NetworkState) : StateChange()
    data class SettingsChange(val settings: AppSettings, val tunnels: Tunnels) : StateChange()
    data class ActiveTunnelsChange(val activeTunnels: Map<TunnelConf, TunnelState>) : StateChange()
}