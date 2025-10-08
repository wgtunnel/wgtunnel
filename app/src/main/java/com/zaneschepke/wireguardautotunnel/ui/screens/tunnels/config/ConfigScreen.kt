package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaneschepke.wireguardautotunnel.data.entity.TunnelConfig
import com.zaneschepke.wireguardautotunnel.ui.LocalSharedVm
import com.zaneschepke.wireguardautotunnel.ui.common.SecureScreenFromRecording
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.config.components.AddPeerButton
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.config.components.InterfaceSection
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.config.components.PeersSection
import com.zaneschepke.wireguardautotunnel.ui.sideeffect.LocalSideEffect
import com.zaneschepke.wireguardautotunnel.ui.state.ConfigProxy
import com.zaneschepke.wireguardautotunnel.ui.state.PeerProxy
import com.zaneschepke.wireguardautotunnel.viewmodel.ConfigViewModel
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun ConfigScreen(viewModel: ConfigViewModel) {
    val sharedViewModel = LocalSharedVm.current

    val configUiState by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    if (configUiState.isLoading) return

    var configProxy by remember {
        mutableStateOf(
            configUiState.tunnel?.let { ConfigProxy.from(it.toAmConfig()) } ?: ConfigProxy()
        )
    }

    var tunnelName by remember { mutableStateOf(configUiState.tunnel?.tunName ?: "") }
    val isGlobalConfig = rememberSaveable { tunnelName == TunnelConfig.GLOBAL_CONFIG_NAME }

    val isTunnelNameTaken by
        remember(tunnelName) {
            derivedStateOf { configUiState.unavailableNames.contains(tunnelName) }
        }

    sharedViewModel.collectSideEffect { sideEffect ->
        if (sideEffect is LocalSideEffect.SaveChanges)
            viewModel.saveConfigProxy(configProxy, tunnelName)
    }

    SecureScreenFromRecording()

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
        modifier =
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
    ) {
        InterfaceSection(
            isGlobalConfig,
            configProxy = configProxy,
            tunnelName,
            isTunnelNameTaken,
            onInterfaceChange = { configProxy = configProxy.copy(`interface` = it) },
            onTunnelNameChange = { tunnelName = it },
            onMimicQuic = {
                configProxy = configProxy.copy(`interface` = configProxy.`interface`.setQuicMimic())
            },
            onMimicDns = {
                configProxy = configProxy.copy(`interface` = configProxy.`interface`.setDnsMimic())
            },
            onMimicSip = {
                configProxy = configProxy.copy(`interface` = configProxy.`interface`.setSipMimic())
            },
        )
        if (!isGlobalConfig)
            PeersSection(
                configProxy,
                onRemove = {
                    configProxy =
                        configProxy.copy(
                            peers = configProxy.peers.toMutableList().apply { removeAt(it) }
                        )
                },
                onToggleLan = { index ->
                    configProxy =
                        configProxy.copy(
                            peers =
                                configProxy.peers.toMutableList().apply {
                                    val peer = get(index)
                                    val updated =
                                        if (peer.isLanExcluded()) peer.includeLan()
                                        else peer.excludeLan()
                                    set(index, updated)
                                }
                        )
                },
                onUpdatePeer = { peer, index ->
                    configProxy =
                        configProxy.copy(
                            peers = configProxy.peers.toMutableList().apply { set(index, peer) }
                        )
                },
            )
        if (!isGlobalConfig)
            AddPeerButton {
                configProxy = configProxy.copy(peers = configProxy.peers + PeerProxy())
            }
    }
}
