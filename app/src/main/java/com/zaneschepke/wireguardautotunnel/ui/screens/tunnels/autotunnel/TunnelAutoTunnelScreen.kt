package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.autotunnel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.autotunnel.components.MobileDataTunnelItem
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.autotunnel.components.WifiTunnelItem
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.autotunnel.components.ethernetTunnelItem
import com.zaneschepke.wireguardautotunnel.viewmodel.TunnelsViewModel

@Composable
fun TunnelAutoTunnelScreen(tunnelId: Int, viewModel: TunnelsViewModel = hiltViewModel()) {
    val tunnelsState by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    if (!tunnelsState.stateInitialized) return

    val tunnelConf by
        remember(tunnelsState.tunnels) {
            derivedStateOf { tunnelsState.tunnels.find { it.id == tunnelId }!! }
        }

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
        modifier =
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
    ) {
        SurfaceSelectionGroupButton(
            items =
                buildList {
                    add(
                        MobileDataTunnelItem(tunnelConf.isMobileDataTunnel) {
                            viewModel.setMobileDataTunnel(tunnelId, it)
                        }
                    )
                    add(
                        ethernetTunnelItem(tunnelConf.isEthernetTunnel) {
                            viewModel.setEthernetTunnel(tunnelId, it)
                        }
                    )
                    add(
                        WifiTunnelItem(
                            tunnelConf.tunnelNetworks,
                            tunnelsState.isWildcardsEnabled,
                            onSaveTunnelNetwork = { viewModel.addTunnelNetwork(tunnelId, it) },
                            onDeleteTunnelNetwork = { viewModel.removeTunnelNetwork(tunnelId, it) },
                        )
                    )
                }
        )
    }
}
