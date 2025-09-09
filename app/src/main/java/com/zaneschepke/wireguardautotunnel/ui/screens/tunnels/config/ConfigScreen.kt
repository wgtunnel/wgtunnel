package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.ui.LocalSharedVm
import com.zaneschepke.wireguardautotunnel.ui.common.SecureScreenFromRecording
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.config.components.AddPeerButton
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.config.components.InterfaceSection
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.config.components.PeersSection
import com.zaneschepke.wireguardautotunnel.ui.sideeffect.LocalSideEffect
import com.zaneschepke.wireguardautotunnel.ui.state.ConfigProxy
import com.zaneschepke.wireguardautotunnel.ui.state.PeerProxy
import com.zaneschepke.wireguardautotunnel.viewmodel.TunnelsViewModel
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun ConfigScreen(tunnelId: Int? = null, viewModel: TunnelsViewModel = hiltViewModel()) {
    val sharedViewModel = LocalSharedVm.current

    val tunnelsState by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    val tunnelConf by
        remember(tunnelsState.tunnels) {
            derivedStateOf { tunnelsState.tunnels.find { it.id == tunnelId } }
        }

    var configProxy by remember {
        mutableStateOf(tunnelConf?.let { ConfigProxy.from(it.toAmConfig()) } ?: ConfigProxy())
    }

    var tunnelName by remember { mutableStateOf(tunnelConf?.tunName ?: "") }

    val isTunnelNameTaken by
        remember(tunnelName, tunnelsState.tunnels) {
            derivedStateOf {
                tunnelsState.tunnels.any { it.tunName == tunnelName && it.id != tunnelConf?.id }
            }
        }

    sharedViewModel.collectSideEffect { sideEffect ->
        if (sideEffect is LocalSideEffect.SaveChanges)
            viewModel.saveConfigProxy(tunnelId, configProxy, tunnelName)
    }

    SecureScreenFromRecording()

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
        modifier =
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 12.dp, bottom = 24.dp)
                .padding(horizontal = 12.dp),
    ) {
        InterfaceSection(
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
        AddPeerButton() { configProxy = configProxy.copy(peers = configProxy.peers + PeerProxy()) }
    }
}
