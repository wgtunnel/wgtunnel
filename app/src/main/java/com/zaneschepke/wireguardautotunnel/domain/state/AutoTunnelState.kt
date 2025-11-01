package com.zaneschepke.wireguardautotunnel.domain.state

import com.zaneschepke.wireguardautotunnel.core.service.autotunnel.ActiveTunnelsChange
import com.zaneschepke.wireguardautotunnel.core.service.autotunnel.NetworkChange
import com.zaneschepke.wireguardautotunnel.core.service.autotunnel.SettingsChange
import com.zaneschepke.wireguardautotunnel.core.service.autotunnel.StateChange
import com.zaneschepke.wireguardautotunnel.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.domain.events.AutoTunnelEvent
import com.zaneschepke.wireguardautotunnel.domain.events.AutoTunnelEvent.DoNothing
import com.zaneschepke.wireguardautotunnel.domain.events.AutoTunnelEvent.Start
import com.zaneschepke.wireguardautotunnel.domain.model.AutoTunnelSettings
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.util.extensions.isMatchingToWildcardList

data class AutoTunnelState(
    val activeTunnels: Map<Int, TunnelState> = emptyMap(),
    val networkState: NetworkState = NetworkState(),
    val settings: AutoTunnelSettings = AutoTunnelSettings(),
    val appMode: AppMode = AppMode.VPN,
    val tunnels: List<TunnelConfig> = emptyList(),
) {

    fun determineAutoTunnelEvent(stateChange: StateChange): AutoTunnelEvent {
        when (stateChange) {
            is NetworkChange,
            is SettingsChange -> {
                // Compute desired tunnel based on network conditions
                var desiredTunnel: TunnelConfig? = null
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
                        return Start(desiredTunnel)
                    }
                } else {
                    if (currentTunnel != null) {
                        return AutoTunnelEvent.Stop
                    }
                }
            }

            is ActiveTunnelsChange -> Unit
        }
        return DoNothing
    }

    // also need to check for Wi-Fi state as there is some overlap when they are both connected
    private fun isMobileDataActive(): Boolean {
        return !networkState.isEthernetConnected &&
            !networkState.isWifiConnected &&
            networkState.isMobileDataConnected
    }

    private fun preferredMobileDataTunnel(): TunnelConfig? {
        return tunnels.firstOrNull { it.isMobileDataTunnel }
            ?: tunnels.firstOrNull { it.isPrimaryTunnel }
            ?: tunnels.firstOrNull()
    }

    private fun preferredEthernetTunnel(): TunnelConfig? {
        return tunnels.firstOrNull { it.isEthernetTunnel }
            ?: tunnels.firstOrNull { it.isPrimaryTunnel }
            ?: tunnels.firstOrNull()
    }

    private fun preferredWifiTunnel(): TunnelConfig? {
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

    private fun getTunnelWithMatchingTunnelNetwork(): TunnelConfig? {
        return networkState.wifiName?.let { wifiName ->
            tunnels.firstOrNull { hasTrustedWifiName(wifiName, it.tunnelNetworks) }
        }
    }
}
