package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.tunneloptions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.LocalIsAndroidTV
import com.zaneschepke.wireguardautotunnel.ui.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.LocalSharedVm
import com.zaneschepke.wireguardautotunnel.ui.common.SectionDivider
import com.zaneschepke.wireguardautotunnel.ui.common.button.ActionIconButton
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.navigation.Route.Config
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.AuthorizationPromptWrapper
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.tunneloptions.components.*
import com.zaneschepke.wireguardautotunnel.ui.state.NavbarState
import com.zaneschepke.wireguardautotunnel.viewmodel.TunnelsViewModel

@Composable
fun TunnelOptionsScreen(tunnelId: Int, viewModel: TunnelsViewModel) {
    val isTv = LocalIsAndroidTV.current
    val navController = LocalNavController.current
    val sharedViewModel = LocalSharedVm.current

    val tunnelsState by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    val tunnelConf by
        remember(tunnelsState.tunnels) {
            derivedStateOf { tunnelsState.tunnels.find { it.id == tunnelId }!! }
        }

    var showAuthPrompt by rememberSaveable { mutableStateOf(!isTv) }
    var isAuthorized by rememberSaveable { mutableStateOf(isTv) }
    var showQrModal by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        sharedViewModel.updateNavbarState(
            NavbarState(
                showBottomItems = true,
                topTitle = { Text(tunnelConf.name) },
                topTrailing = {
                    Row {
                        ActionIconButton(
                            Icons.Rounded.QrCode2,
                            com.zaneschepke.wireguardautotunnel.R.string.show_qr,
                        ) {
                            showQrModal = true
                        }
                        ActionIconButton(Icons.Rounded.Edit, R.string.edit_tunnel) {
                            navController.navigate(Config(tunnelId))
                        }
                    }
                },
            )
        )
    }

    if (showQrModal) {

        // Show authorization prompt if needed
        if (showAuthPrompt) {
            AuthorizationPromptWrapper(
                onDismiss = {
                    showAuthPrompt = false
                    showQrModal = false
                },
                onSuccess = {
                    showAuthPrompt = false
                    isAuthorized = true
                },
            )
        }
        if (isAuthorized) {
            QrCodeDialog(tunnelConf = tunnelConf, onDismiss = { showQrModal = false })
        }
    }

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
        modifier =
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 24.dp)
                .padding(horizontal = 12.dp),
    ) {
        SurfaceSelectionGroupButton(
            items =
                listOf(
                    PrimaryTunnelItem(tunnelConf) { viewModel.togglePrimaryTunnel(tunnelId) },
                    AutoTunnelingItem(tunnelConf, navController),
                    serverIpv4Item(tunnelConf) { viewModel.toggleIpv4Preferred(tunnelId) },
                    SplitTunnelingItem(tunnelConf, navController),
                )
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
