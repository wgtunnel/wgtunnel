package com.zaneschepke.wireguardautotunnel.domain.state

import com.zaneschepke.wireguardautotunnel.core.service.autotunnel.ActiveTunnelsChange
import com.zaneschepke.wireguardautotunnel.core.service.autotunnel.NetworkChange
import com.zaneschepke.wireguardautotunnel.core.service.autotunnel.SettingsChange
import com.zaneschepke.wireguardautotunnel.core.service.autotunnel.StateChange
import com.zaneschepke.wireguardautotunnel.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.domain.events.AutoTunnelEvent
import com.zaneschepke.wireguardautotunnel.domain.events.AutoTunnelEvent.DoNothing
import com.zaneschepke.wireguardautotunnel.domain.events.AutoTunnelEvent.Start
import com.zaneschepke.wireguardautotunnel.domain.events.AutoTunnelEvent.Restart
import com.zaneschepke.wireguardautotunnel.domain.model.AutoTunnelSettings
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.util.extensions.isMatchingToWildcardList
import timber.log.Timber

data class AutoTunnelState(
    val activeTunnels: Map<Int, TunnelState> = emptyMap(),
    val networkState: NetworkState = NetworkState(),
    val settings: AutoTunnelSettings = AutoTunnelSettings(),
    val appMode: AppMode = AppMode.VPN,
    val tunnels: List<TunnelConfig> = emptyList(),
) {

    fun determineAutoTunnelEvent(stateChange: StateChange, oldState: AutoTunnelState? = null): AutoTunnelEvent {
        when (stateChange) {
            is NetworkChange,
            is SettingsChange -> {
                val currentTunnelId = activeTunnels.entries.firstOrNull()?.key

                // --- ROAMING LOGIC ---
                if (settings.isBssidRoamingEnabled && stateChange is NetworkChange && currentTunnelId != null && oldState != null) {
                    val oldNet = oldState.networkState.activeNetwork
                    val newNet = this.networkState.activeNetwork

                    if (oldNet is ActiveNetwork.Wifi && newNet is ActiveNetwork.Wifi) {
                        
                        // BOOTSTRAP: Allow first save if list is empty & auto-save ON
                        val isListIgnored = settings.roamingSSIDs.isEmpty() && settings.isBssidAutoSaveEnabled

                        val isSsidAllowed = !settings.isBssidListEnabled || 
                                            isListIgnored || 
                                            hasMatch(newNet.ssid, settings.roamingSSIDs, settings.isBssidWildcardsEnabled)

                        val isOldValid = !oldNet.bssid.isNullOrBlank() && oldNet.bssid != "02:00:00:00:00:00" && oldNet.bssid != "00:00:00:00:00:00"
                        val isNewValid = !newNet.bssid.isNullOrBlank() && newNet.bssid != "02:00:00:00:00:00" && newNet.bssid != "00:00:00:00:00:00"

                        // Trigger Restart ONLY on BSSID change within same SSID
                        if (isSsidAllowed && oldNet.ssid == newNet.ssid && oldNet.bssid != newNet.bssid && isOldValid && isNewValid) {
                            Timber.d("Roaming detected: ${oldNet.bssid} -> ${newNet.bssid}. Bootstrap: $isListIgnored")
                            val activeConfig = tunnels.find { it.id == currentTunnelId }
                            if (activeConfig != null) {
                                return Restart(activeConfig)
                            }
                        }
                    }
                }
                // --------------------------

                // --- STANDARD LOGIC ---
                var preferredTunnel: TunnelConfig? = null
                if (ethernetActive && settings.isTunnelOnEthernetEnabled) {
                    preferredTunnel = preferredEthernetTunnel()
                } else if (mobileDataActive && settings.isTunnelOnMobileDataEnabled) {
                    preferredTunnel = preferredMobileDataTunnel()
                } else if (wifiActive && settings.isTunnelOnWifiEnabled && !isWifiTrusted()) {
                    preferredTunnel = preferredWifiTunnel()
                }

                if (!networkState.hasInternet() && settings.isStopOnNoInternetEnabled) {
                    preferredTunnel = null
                }

                if (preferredTunnel != null) {
                    if (currentTunnelId != preferredTunnel.id) {
                        return Start(preferredTunnel)
                    }
                } else {
                    if (currentTunnelId != null) {
                        return AutoTunnelEvent.Stop
                    }
                }
            }

            is ActiveTunnelsChange -> Unit
        }
        return DoNothing
    }

    private fun hasMatch(
        wifiName: String,
        wifiNames: Set<String>,
        useWildcards: Boolean,
    ): Boolean {
        return if (useWildcards) {
            wifiNames.isMatchingToWildcardList(wifiName)
        } else {
            wifiNames.contains(wifiName)
        }
    }

    private val ethernetActive: Boolean = networkState.activeNetwork is ActiveNetwork.Ethernet
    private val mobileDataActive: Boolean = networkState.activeNetwork is ActiveNetwork.Cellular
    private val wifiActive: Boolean = networkState.activeNetwork is ActiveNetwork.Wifi

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
        return getTunnelWithMappedNetwork()
            ?: tunnels.firstOrNull { it.isPrimaryTunnel }
            ?: tunnels.firstOrNull()
    }

    private fun isTrustedNetwork(ssid: String): Boolean =
        hasMatch(ssid, settings.trustedNetworkSSIDs, settings.isWildcardsEnabled)

    private fun isWifiTrusted(): Boolean {
        return with(networkState.activeNetwork) {
            this is ActiveNetwork.Wifi && isTrustedNetwork(this.ssid)
        }
    }

    private fun getTunnelWithMappedNetwork(): TunnelConfig? =
        when (val network = networkState.activeNetwork) {
            is ActiveNetwork.Wifi ->
                tunnels.firstOrNull { hasMatch(network.ssid, it.tunnelNetworks, settings.isWildcardsEnabled) }
            else -> null
        }
}
