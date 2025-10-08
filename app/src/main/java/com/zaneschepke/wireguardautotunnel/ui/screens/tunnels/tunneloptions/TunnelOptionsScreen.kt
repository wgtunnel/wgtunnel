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
import com.zaneschepke.wireguardautotunnel.viewmodel.TunnelViewModel
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun TunnelOptionsScreen(viewModel: TunnelViewModel) {
    val navController = LocalNavController.current
    val sharedViewModel = LocalSharedVm.current

    val tunnelState by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    if (tunnelState.isLoading) return
    val tunnel = tunnelState.tunnel ?: return

    var showQrModal by rememberSaveable { mutableStateOf(false) }

    sharedViewModel.collectSideEffect { sideEffect ->
        if (sideEffect is LocalSideEffect.Modal.QR) showQrModal = true
    }

    if (showQrModal) {
        QrCodeDialog(tunnelConf = tunnel, onDismiss = { showQrModal = false })
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
                    add(primaryTunnelItem(tunnel) { viewModel.togglePrimaryTunnel() })
                    add(autoTunnelingItem(tunnel))
                    add(
                        splitTunnelingItem(stringResource(R.string.splt_tunneling)) {
                            navController.push(Route.SplitTunnel(id = tunnel.id))
                        }
                    )
                }
        )
        SectionDivider()
        SurfaceSelectionGroupButton(
            items =
                buildList {
                    add(
                        dynamicDnsItem(tunnel.restartOnPingFailure) {
                            viewModel.setRestartOnPing(it)
                        }
                    )
                    if (tunnelState.appMode != AppMode.KERNEL)
                        add(
                            preferIpv6Item(!tunnel.isIpv4Preferred) {
                                viewModel.toggleIpv4Preferred()
                            }
                        )
                }
        )
        if (tunnelState.isPingEnabled) {
            SectionDivider()
            SurfaceSelectionGroupButton(
                items = listOf(pingConfigItem(tunnel) { ip -> viewModel.setTunnelPingIp(ip) })
            )
        }
    }
}
