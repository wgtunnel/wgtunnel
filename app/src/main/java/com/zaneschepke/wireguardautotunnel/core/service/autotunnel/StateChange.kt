package com.zaneschepke.wireguardautotunnel.core.service.autotunnel

import com.zaneschepke.wireguardautotunnel.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.domain.model.AutoTunnelSettings
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.domain.state.NetworkState
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState

sealed class StateChange {
    data class NetworkChange(val networkState: NetworkState) : StateChange()

    data class SettingsChange(
        val appMode: AppMode,
        val settings: AutoTunnelSettings,
        val tunnels: List<TunnelConfig>,
    ) : StateChange()

    data class ActiveTunnelsChange(val activeTunnels: Map<Int, TunnelState>) : StateChange()
}
