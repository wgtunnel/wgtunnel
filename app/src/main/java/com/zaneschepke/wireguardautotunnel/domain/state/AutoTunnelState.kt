package com.zaneschepke.wireguardautotunnel.domain.state

import com.zaneschepke.wireguardautotunnel.core.service.autotunnel.StateChange
import com.zaneschepke.wireguardautotunnel.domain.events.AutoTunnelEvent
import com.zaneschepke.wireguardautotunnel.domain.events.AutoTunnelEvent.DoNothing
import com.zaneschepke.wireguardautotunnel.domain.events.AutoTunnelEvent.Start
import com.zaneschepke.wireguardautotunnel.domain.model.GeneralSettings
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.util.extensions.isMatchingToWildcardList

data class AutoTunnelState(
    val activeTunnels: Map<Int, TunnelState> = emptyMap(),
    val networkState: NetworkState = NetworkState(),
    val settings: GeneralSettings = GeneralSettings(),
    val tunnels: List<TunnelConf> = emptyList(),
) {

    fun determineAutoTunnelEvent(stateChange: StateChange): AutoTunnelEvent {
        when (stateChange) {
            is StateChange.NetworkChange,
            is StateChange.SettingsChange -> {
                // Compute desired tunnel based on network conditions
                var desiredTunnel: TunnelConf? = null
                if (networkState.isEthernetConnected && settings.isTunnelOnEthernetEnabled) {
                    desiredTunnel = preferredEthernetTunnel()
                } else if (isMobileDataActive() && settings.isTunnelOnMobileDataEnabled) {
                    desiredTunnel = preferredMobileDataTunnel()
                } else if (
                    isWifiActive() && settings.isTunnelOnWifiEnabled && !isCurrentSSIDTrusted()
                ) {
                    desiredTunnel = preferredWifiTunnel()
                }

                // Override for no connectivity if enabled
                if (isNoConnectivity() && settings.isStopOnNoInternetEnabled) {
                    desiredTunnel = null
                }

                // Determine current active tunnel (assuming only one can be active)
                val currentTunnel = activeTunnels.entries.firstOrNull()?.key

                // Handle tunnel start/stop/change
                if (desiredTunnel != null) {
                    if (currentTunnel != desiredTunnel.id) {
                        // Start or switch to the desired tunnel (overrides any kill switch)
                        return Start(desiredTunnel)
                    }
                    // If already active and matching, fall through to kill switch check (though
                    // unlikely needed)
                } else {
                    if (currentTunnel != null) {
                        // Stop the active tunnel (then next emission can handle kill switch if
                        // needed)
                        return AutoTunnelEvent.Stop
                    }
                }
            }

            is StateChange.ActiveTunnelsChange -> Unit
        }
        return DoNothing
    }

    // also need to check for Wi-Fi state as there is some overlap when they are both connected
    private fun isMobileDataActive(): Boolean {
        return !networkState.isEthernetConnected &&
            !networkState.isWifiConnected &&
            networkState.isMobileDataConnected
    }

    private fun preferredMobileDataTunnel(): TunnelConf? {
        return tunnels.firstOrNull { it.isMobileDataTunnel }
            ?: tunnels.firstOrNull { it.isPrimaryTunnel }
            ?: tunnels.firstOrNull()
    }

    private fun preferredEthernetTunnel(): TunnelConf? {
        return tunnels.firstOrNull { it.isEthernetTunnel }
            ?: tunnels.firstOrNull { it.isPrimaryTunnel }
            ?: tunnels.firstOrNull()
    }

    private fun preferredWifiTunnel(): TunnelConf? {
        return getTunnelWithMatchingTunnelNetwork()
            ?: tunnels.firstOrNull { it.isPrimaryTunnel }
            ?: tunnels.firstOrNull()
    }

    // ignore cellular state as there is overlap where it may still be active, but not prioritized
    private fun isWifiActive(): Boolean {
        return !networkState.isEthernetConnected && networkState.isWifiConnected
    }

    private fun isNoConnectivity(): Boolean {
        return !networkState.isEthernetConnected &&
            !networkState.isWifiConnected &&
            !networkState.isMobileDataConnected
    }

    private fun isCurrentSSIDTrusted(): Boolean {
        return networkState.wifiName?.let { hasTrustedWifiName(it) } == true
    }

    private fun hasTrustedWifiName(
        wifiName: String,
        wifiNames: Set<String> = settings.trustedNetworkSSIDs,
    ): Boolean {
        return if (settings.isWildcardsEnabled) {
            wifiNames.isMatchingToWildcardList(wifiName)
        } else {
            wifiNames.contains(wifiName)
        }
    }

    private fun getTunnelWithMatchingTunnelNetwork(): TunnelConf? {
        return networkState.wifiName?.let { wifiName ->
            tunnels.firstOrNull { hasTrustedWifiName(wifiName, it.tunnelNetworks) }
        }
    }
}
