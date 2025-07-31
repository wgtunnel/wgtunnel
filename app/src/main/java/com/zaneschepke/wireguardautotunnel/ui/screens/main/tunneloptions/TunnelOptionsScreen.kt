package com.zaneschepke.wireguardautotunnel.ui.screens.main.tunneloptions

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
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.ui.common.SectionDivider
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.navigation.LocalIsAndroidTV
import com.zaneschepke.wireguardautotunnel.ui.screens.main.tunneloptions.components.*
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.AuthorizationPromptWrapper
import com.zaneschepke.wireguardautotunnel.ui.state.AppViewState
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun TunnelOptionsScreen(
    tunnelConf: TunnelConf,
    viewModel: AppViewModel,
    appViewState: AppViewState,
) {
    val isTv = LocalIsAndroidTV.current

    var showAuthPrompt by remember { mutableStateOf(!isTv) }
    var isAuthorized by remember { mutableStateOf(isTv) }

    if (appViewState.showModal == AppViewState.ModalType.QR) {

        // Show authorization prompt if needed
        if (showAuthPrompt) {
            AuthorizationPromptWrapper(
                onDismiss = { showAuthPrompt = false },
                onSuccess = {
                    showAuthPrompt = false
                    isAuthorized = true
                },
                viewModel = viewModel,
            )
        }
        if (isAuthorized) {
            QrCodeDialog(
                tunnelConf = tunnelConf,
                onDismiss = {
                    viewModel.handleEvent(AppEvent.SetShowModal(AppViewState.ModalType.NONE))
                },
            )
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
                    PrimaryTunnelItem(tunnelConf, viewModel),
                    AutoTunnelingItem(tunnelConf),
                    serverIpv4Item(tunnelConf, viewModel),
                    SplitTunnelingItem(tunnelConf),
                )
        )
        SectionDivider()
        SurfaceSelectionGroupButton(
            items =
                buildList {
                    add(PingRestartItem(tunnelConf, viewModel))
                    if (tunnelConf.isPingEnabled) {
                        add(PingConfigItem(tunnelConf, viewModel))
                    }
                }
        )
    }
}
