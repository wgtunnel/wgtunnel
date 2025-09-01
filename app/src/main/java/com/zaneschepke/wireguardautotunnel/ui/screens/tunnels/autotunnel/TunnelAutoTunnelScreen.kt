package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.autotunnel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.ui.LocalSharedVm
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.autotunnel.components.MobileDataTunnelItem
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.autotunnel.components.PingRestartItem
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.autotunnel.components.WifiTunnelItem
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.autotunnel.components.ethernetTunnelItem
import com.zaneschepke.wireguardautotunnel.ui.state.NavbarState
import com.zaneschepke.wireguardautotunnel.viewmodel.TunnelsViewModel

@Composable
fun TunnelAutoTunnelScreen(tunnelId: Int, viewModel: TunnelsViewModel) {
    val sharedViewModel = LocalSharedVm.current
    val tunnelsState by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    val tunnelConf by
        remember(tunnelsState.tunnels) {
            derivedStateOf { tunnelsState.tunnels.find { it.id == tunnelId }!! }
        }

    LaunchedEffect(Unit) {
        sharedViewModel.updateNavbarState(
            NavbarState(showBottomItems = true, topTitle = { Text(tunnelConf.name) })
        )
    }

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
        modifier =
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp)
                .padding(horizontal = 12.dp),
    ) {
        SurfaceSelectionGroupButton(
            items =
                buildList {
                    if (tunnelsState.isPingEnabled) {
                        add(
                            PingRestartItem(tunnelConf.restartOnPingFailure) {
                                viewModel.setRestartOnPing(tunnelId, it)
                            }
                        )
                    }
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
