package com.zaneschepke.wireguardautotunnel.core.service.autotunnel

import com.zaneschepke.wireguardautotunnel.domain.model.GeneralSettings
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.state.NetworkState
import com.zaneschepke.wireguardautotunnel.domain.state.PingState
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState
import com.zaneschepke.wireguardautotunnel.util.extensions.Tunnels
import org.amnezia.awg.crypto.Key

sealed class StateChange {
    data class NetworkChange(val networkState: NetworkState) : StateChange()

    data class SettingsChange(val settings: GeneralSettings, val tunnels: Tunnels) : StateChange()

    data class ActiveTunnelsChange(val activeTunnels: Map<TunnelConf, TunnelState>) : StateChange()

    data class MonitoringChange(val pingStates: Map<TunnelConf, Map<Key, PingState>?>) :
        StateChange()
}
