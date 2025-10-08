package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.autotunnel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.autotunnel.components.MobileDataTunnelItem
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.autotunnel.components.WifiTunnelItem
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.autotunnel.components.ethernetTunnelItem
import com.zaneschepke.wireguardautotunnel.viewmodel.TunnelViewModel

@Composable
fun TunnelAutoTunnelScreen(viewModel: TunnelViewModel) {
    val tunnelState by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    if (tunnelState.isLoading) return
    val tunnel = tunnelState.tunnel ?: return

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
                        MobileDataTunnelItem(tunnel.isMobileDataTunnel) {
                            viewModel.setMobileDataTunnel(it)
                        }
                    )
                    add(
                        ethernetTunnelItem(tunnel.isEthernetTunnel) {
                            viewModel.setEthernetTunnel(it)
                        }
                    )
                    add(
                        WifiTunnelItem(
                            tunnel.tunnelNetworks,
                            isWildcardsEnabled = tunnelState.isWildcardsEnabled,
                            onSaveTunnelNetwork = { viewModel.addTunnelNetwork(it) },
                            onDeleteTunnelNetwork = { viewModel.removeTunnelNetwork(it) },
                        )
                    )
                }
        )
    }
}
