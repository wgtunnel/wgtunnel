package com.zaneschepke.wireguardautotunnel.ui.screens.main.config

import androidx.lifecycle.ViewModel
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.ui.screens.main.config.state.ConfigUiState
import com.zaneschepke.wireguardautotunnel.ui.state.ConfigProxy
import com.zaneschepke.wireguardautotunnel.ui.state.InterfaceProxy
import com.zaneschepke.wireguardautotunnel.ui.state.PeerProxy
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class ConfigViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ConfigUiState())
    val uiState: StateFlow<ConfigUiState> = _uiState.asStateFlow()

    fun initFromTunnel(tunnelConf: TunnelConf?) {
        if (tunnelConf == null) return
        _uiState.update {
            val proxy = ConfigProxy.from(tunnelConf.toAmConfig())
            it.copy(
                tunnelName = tunnelConf.name,
                configProxy = proxy,
                showScripts = proxy.hasScripts(),
                showAmneziaValues = proxy.`interface`.junkPacketCount.isNotBlank(),
                isAuthenticated = false,
            )
        }
    }

    fun updateTunnelName(name: String) {
        _uiState.update { it.copy(tunnelName = name) }
    }

    fun updateInterface(newInterface: InterfaceProxy) {
        _uiState.update { it.copy(configProxy = it.configProxy.copy(`interface` = newInterface)) }
    }

    fun toggleAmneziaValues() {
        _uiState.update { it.copy(showAmneziaValues = !it.showAmneziaValues) }
    }

    fun toggleScripts() {
        _uiState.update { it.copy(showScripts = !it.showScripts) }
    }

    fun toggleAmneziaCompatibility() {
        val (show, `interface`) =
            with(_uiState.value.configProxy) {
                if (`interface`.isAmneziaCompatibilityModeSet()) {
                    Pair(false, `interface`.resetAmneziaProperties())
                } else {
                    Pair(true, `interface`.toAmneziaCompatibilityConfig())
                }
            }
        _uiState.update {
            it.copy(
                showAmneziaValues = show,
                configProxy = it.configProxy.copy(`interface` = `interface`),
            )
        }
    }

    fun addPeer() {
        _uiState.update { currentState ->
            currentState.copy(
                configProxy =
                    currentState.configProxy.copy(
                        peers = currentState.configProxy.peers + PeerProxy()
                    )
            )
        }
    }

    fun removePeer(index: Int) {
        _uiState.update { currentState ->
            currentState.copy(
                configProxy =
                    currentState.configProxy.copy(
                        peers =
                            currentState.configProxy.peers.toMutableList().apply { removeAt(index) }
                    )
            )
        }
    }

    fun updatePeer(index: Int, peer: PeerProxy) {
        _uiState.update { currentState ->
            currentState.copy(
                configProxy =
                    currentState.configProxy.copy(
                        peers =
                            currentState.configProxy.peers.toMutableList().apply {
                                set(index, peer)
                            }
                    )
            )
        }
    }

    fun toggleLanExclusion(index: Int) {
        val peer = _uiState.value.configProxy.peers[index]
        val updated = if (peer.isLanExcluded()) peer.includeLan() else peer.excludeLan()
        updatePeer(index, updated)
    }

    fun onAuthenticated() {
        _uiState.update { it.copy(isAuthenticated = true) }
    }

    fun toggleShowAuthPrompt() {
        _uiState.update { it.copy(showAuthPrompt = !it.showAuthPrompt) }
    }
}
