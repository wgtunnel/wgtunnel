package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.tunneloptions

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.ui.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.LocalSharedVm
import com.zaneschepke.wireguardautotunnel.ui.common.SectionDivider
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.tunneloptions.components.*
import com.zaneschepke.wireguardautotunnel.ui.sideeffect.LocalSideEffect
import com.zaneschepke.wireguardautotunnel.viewmodel.TunnelsViewModel
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun TunnelOptionsScreen(tunnelId: Int, viewModel: TunnelsViewModel = hiltViewModel()) {
    val navController = LocalNavController.current
    val sharedViewModel = LocalSharedVm.current

    val tunnelsState by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    if (!tunnelsState.stateInitialized) return

    val tunnelConf by
        remember(tunnelsState.tunnels) {
            derivedStateOf { tunnelsState.tunnels.find { it.id == tunnelId }!! }
        }

    val ipv6Preferred by
        remember(tunnelConf.isIpv4Preferred) { mutableStateOf(!tunnelConf.isIpv4Preferred) }

    var showQrModal by rememberSaveable { mutableStateOf(false) }

    sharedViewModel.collectSideEffect { sideEffect ->
        if (sideEffect is LocalSideEffect.Modal.QR) showQrModal = true
    }

    if (showQrModal) {
        QrCodeDialog(tunnelConf = tunnelConf, onDismiss = { showQrModal = false })
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
                    add(primaryTunnelItem(tunnelConf) { viewModel.togglePrimaryTunnel(tunnelId) })
                    add(autoTunnelingItem(tunnelConf))
                    add(
                        splitTunnelingItem(stringResource(R.string.splt_tunneling)) {
                            navController.push(Route.SplitTunnel(id = tunnelConf.id))
                        }
                    )
                }
        )
        SectionDivider()
        SurfaceSelectionGroupButton(
            items =
                buildList {
                    add(
                        dynamicDnsItem(tunnelConf.restartOnPingFailure) {
                            viewModel.setRestartOnPing(tunnelId, it)
                        }
                    )
                    if (tunnelsState.appMode != AppMode.KERNEL)
                        add(
                            preferIpv6Item(ipv6Preferred) {
                                viewModel.toggleIpv4Preferred(tunnelId)
                            }
                        )
                }
        )
        if (tunnelsState.isPingEnabled) {
            SectionDivider()
            SurfaceSelectionGroupButton(
                items =
                    listOf(
                        pingConfigItem(tunnelConf) { ip -> viewModel.setTunnelPingIp(ip, tunnelId) }
                    )
            )
        }
    }
}
