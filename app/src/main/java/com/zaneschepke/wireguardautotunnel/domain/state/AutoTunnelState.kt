package com.zaneschepke.wireguardautotunnel.domain.state

import com.zaneschepke.wireguardautotunnel.core.service.autotunnel.AutoTunnelService
import com.zaneschepke.wireguardautotunnel.core.service.autotunnel.StateChange
import com.zaneschepke.wireguardautotunnel.domain.events.AutoTunnelEvent
import com.zaneschepke.wireguardautotunnel.domain.events.AutoTunnelEvent.*
import com.zaneschepke.wireguardautotunnel.domain.model.AppSettings
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.util.extensions.isMatchingToWildcardList

data class AutoTunnelState(
    val activeTunnels: Map<TunnelConf, TunnelState> = emptyMap(),
    val networkState: NetworkState = NetworkState(),
    val settings: AppSettings = AppSettings(),
    val tunnels: List<TunnelConf> = emptyList(),
) {

    fun determineAutoTunnelEvent(stateChange: StateChange): AutoTunnelEvent {
        when(val change = stateChange) {
            is StateChange.NetworkChange, is StateChange.SettingsChange -> {
                // Compute desired tunnel based on network conditions
                var desiredTunnel: TunnelConf? = null
                if (networkState.isEthernetConnected && settings.isTunnelOnEthernetEnabled) {
                    desiredTunnel = preferredEthernetTunnel()
                } else if (isMobileDataActive() && settings.isTunnelOnMobileDataEnabled) {
                    desiredTunnel = preferredMobileDataTunnel()
                } else if (isWifiActive() && settings.isTunnelOnWifiEnabled && !isCurrentSSIDTrusted()) {
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
                    if (currentTunnel != desiredTunnel) {
                        // Start or switch to the desired tunnel (overrides any kill switch)
                        return Start(desiredTunnel)
                    }
                    // If already active and matching, fall through to kill switch check (though unlikely needed)
                } else {
                    if (currentTunnel != null) {
                        // Stop the active tunnel (then next emission can handle kill switch if needed)
                        return AutoTunnelEvent.Stop
                    }
                }
                // Handle kill switch only if no user tunnel is or will be active
                if (stopKillSwitchOnTrusted()) {
                    return AutoTunnelEvent.StopKillSwitch
                }
                if (startKillSwitch()) {
                    val allowedIps =
                        if (settings.isLanOnKillSwitchEnabled) TunnelConf.LAN_BYPASS_ALLOWED_IPS
                        else emptyList()
                    return StartKillSwitch(allowedIps)
                }
            }
            is StateChange.MonitoringChange -> {
                val bounceTunnels = bounceOnPingFailed(change.consecutiveFailures)
                if (bounceTunnels.isNotEmpty()) {
                    return Bounce(bounceTunnels)
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
            ?: tunnels.firstOrNull { it.isPrimaryTunnel } ?: tunnels.firstOrNull()
    }

    private fun preferredEthernetTunnel(): TunnelConf? {
        return tunnels.firstOrNull { it.isEthernetTunnel }
            ?: tunnels.firstOrNull { it.isPrimaryTunnel } ?: tunnels.firstOrNull()
    }

    private fun preferredWifiTunnel(): TunnelConf? {
        return getTunnelWithMatchingTunnelNetwork() ?: tunnels.firstOrNull { it.isPrimaryTunnel } ?: tunnels.firstOrNull()
    }

    // ignore cellular state as there is overlap where it may still be active, but not prioritized
    private fun isWifiActive(): Boolean {
        return !networkState.isEthernetConnected && networkState.isWifiConnected
    }

    private fun stopKillSwitchOnTrusted(): Boolean {
        return networkState.isWifiConnected &&
                settings.isVpnKillSwitchEnabled &&
                settings.isDisableKillSwitchOnTrustedEnabled &&
                isCurrentSSIDTrusted()
    }

    private fun startKillSwitch(): Boolean {
        return settings.isVpnKillSwitchEnabled &&
                (!settings.isDisableKillSwitchOnTrustedEnabled || !isCurrentSSIDTrusted())
    }

    private fun isNoConnectivity(): Boolean {
        return !networkState.isEthernetConnected &&
                !networkState.isWifiConnected &&
                !networkState.isMobileDataConnected
    }

    private fun bounceOnPingFailed(failures: Map<TunnelConf, Int>) : List<Triple<TunnelConf, Map<String, String?>, Int>> {
        return activeTunnels.entries.filter { (tunnel, state) ->
            tunnel.restartOnPingFailure &&
                    (state.pingStates?.any { (key , pingState) ->
                        pingState.let { pingState ->
                            (failures[tunnel] ?: 0) >= AutoTunnelService.CONSECUTIVE_FAILURE_THRESHOLD &&
                                    pingState.failureReason == FailureReason.PingFailed
                        }
                    } ?: false)
        }.map { (tunnel, state) ->
            val maxFailures = state.pingStates?.maxOfOrNull { (key, pingState) ->
                failures[tunnel] ?: 0
            } ?: 0
            val peerMap = (state.statistics?.getPeers()?.associate { peerKey ->
                peerKey.toBase64() to state.statistics.peerStats(peerKey)?.resolvedEndpoint
            } ?: emptyMap())
            Triple(tunnel, peerMap, maxFailures)
        }
    }

    private fun isCurrentSSIDTrusted(): Boolean {
        return networkState.wifiName?.let { hasTrustedWifiName(it) } == true
    }

    private fun hasTrustedWifiName(
        wifiName: String,
        wifiNames: List<String> = settings.trustedNetworkSSIDs,
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