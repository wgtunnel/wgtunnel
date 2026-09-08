package com.zaneschepke.wireguardautotunnel.service.autotunnel

import com.wgtunnel.backend.autotunnel.AutoTunnelDecision
import com.wgtunnel.backend.autotunnel.AutoTunnelEngine as CoreEngine
import com.wgtunnel.backend.autotunnel.AutoTunnelNetwork
import com.wgtunnel.backend.autotunnel.AutoTunnelNetworkType
import com.wgtunnel.backend.autotunnel.AutoTunnelPolicy
import com.wgtunnel.backend.autotunnel.AutoTunnelSnapshot
import com.wgtunnel.backend.autotunnel.AutoTunnelTunnel
import com.zaneschepke.networkmonitor.ActiveNetwork
import com.zaneschepke.wireguardautotunnel.domain.events.AutoTunnelEvent
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.domain.state.AutoTunnelState

class AutoTunnelEngine {
    private val core = CoreEngine()

    fun evaluate(state: AutoTunnelState): AutoTunnelEvent {
        return when (val decision = core.evaluate(state.toCore())) {
            is AutoTunnelDecision.Sync -> {
                AutoTunnelEvent.Sync(
                    start = state.tunnels.filter { it.id.toLong() in decision.start }.toSet(),
                    stop = decision.stop.map { it.toInt() }.toSet(),
                )
            }
            AutoTunnelDecision.DoNothing -> AutoTunnelEvent.DoNothing
            AutoTunnelDecision.StopAllDueToNoInternet -> AutoTunnelEvent.StopAllDueToNoInternet
        }
    }

    private fun AutoTunnelState.toCore(): AutoTunnelSnapshot {
        val wifi = networkState.activeNetwork as? ActiveNetwork.Wifi
        return AutoTunnelSnapshot(
            network =
                AutoTunnelNetwork(
                    type =
                        when (networkState.activeNetwork) {
                            is ActiveNetwork.Wifi -> AutoTunnelNetworkType.WIFI
                            is ActiveNetwork.Ethernet -> AutoTunnelNetworkType.ETHERNET
                            is ActiveNetwork.Cellular -> AutoTunnelNetworkType.CELLULAR
                            is ActiveNetwork.Disconnected -> AutoTunnelNetworkType.DISCONNECTED
                        },
                    ssid = wifi?.ssid.orEmpty(),
                    bssid = wifi?.bssid.orEmpty(),
                    hasUsableNetwork = networkState.hasUsableNetwork,
                    // Debounced. Reacts to detected immediately but only cleared once false has
                    // held stable for a duration
                    captivePortal = wifi != null && confirmedCaptivePortal,
                ),
            policy =
                AutoTunnelPolicy(
                    isTunnelOnWifiEnabled = settings.isTunnelOnWifiEnabled,
                    isTunnelOnEthernetEnabled = settings.isTunnelOnEthernetEnabled,
                    isTunnelOnMobileDataEnabled = settings.isTunnelOnMobileDataEnabled,
                    isWildcardsEnabled = settings.isWildcardsEnabled,
                    isStopOnNoInternetEnabled = settings.isStopOnNoInternetEnabled,
                    disableTunnelOnCaptivePortal = settings.disableTunnelOnCaptivePortal,
                    trustedNetworkSsids = settings.trustedNetworkSSIDs,
                    trustedNetworkBssids = settings.trustedNetworkBSSIDs,
                ),
            tunnels = tunnels.map { it.toCore() },
            activeTunnelIds = backendStatus.activeTunnels.keys.map { it.toLong() }.toSet(),
        )
    }

    private fun TunnelConfig.toCore(): AutoTunnelTunnel {
        return AutoTunnelTunnel(
            id = id.toLong(),
            name = name,
            isPrimaryTunnel = isPrimaryTunnel,
            isEthernetTunnel = isEthernetTunnel,
            isMobileDataTunnel = isMobileDataTunnel,
            tunnelNetworks = tunnelNetworks,
            tunnelBssids = tunnelBSSIDs,
        )
    }
}
