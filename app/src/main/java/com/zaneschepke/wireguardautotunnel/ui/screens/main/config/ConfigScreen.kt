package com.zaneschepke.wireguardautotunnel.ui.screens.main.config

import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.MainActivity
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.ui.common.prompt.AuthorizationPrompt
import com.zaneschepke.wireguardautotunnel.ui.screens.main.config.components.AddPeerButton
import com.zaneschepke.wireguardautotunnel.ui.screens.main.config.components.InterfaceSection
import com.zaneschepke.wireguardautotunnel.ui.screens.main.config.components.PeersSection
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.util.StringValue
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun ConfigScreen(
    tunnelConf: TunnelConf?,
    appUiState: AppUiState,
    appViewModel: AppViewModel,
    viewModel: ConfigViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val activity = context as? MainActivity

    var save by remember { mutableStateOf(false) }

    val isTunnelNameTaken by
        remember(uiState.tunnelName, appUiState.tunnels) {
            derivedStateOf {
                appUiState.tunnels
                    .filter { it.id != tunnelConf?.id }
                    .any { it.name == uiState.tunnelName }
            }
        }

    // Secure screen due to sensitive information
    DisposableEffect(Unit) {
        activity
            ?.window
            ?.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }

    LaunchedEffect(Unit) {
        // set callback for navbar to invoke save
        appViewModel.handleEvent(
            AppEvent.SetScreenAction {
                keyboardController?.hide()
                if (!isTunnelNameTaken) {
                    save = true
                }
            }
        )
    }

    LaunchedEffect(tunnelConf) { viewModel.initFromTunnel(tunnelConf) }

    // TODO improve error messages
    LaunchedEffect(save) {
        if (save) {
            try {
                appViewModel.handleEvent(
                    AppEvent.SaveTunnelUniquely(
                        uiState.configProxy.buildTunnelConfFromState(uiState.tunnelName, tunnelConf)
                    )
                )
                appViewModel.handleEvent(
                    AppEvent.ShowMessage(StringValue.StringResource(R.string.config_changes_saved))
                )
                appViewModel.handleEvent(AppEvent.PopBackStack(true))
            } catch (e: Exception) {
                val message = e.message ?: context.resources.getString(R.string.unknown_error)
                appViewModel.handleEvent(AppEvent.ShowMessage(StringValue.DynamicString(message)))
            } finally {
                save = false
            }
        }
    }

    if (uiState.showAuthPrompt) {
        AuthorizationPrompt(
            onSuccess = {
                viewModel.toggleShowAuthPrompt()
                viewModel.onAuthenticated()
            },
            onError = {
                viewModel.toggleShowAuthPrompt()
                appViewModel.handleEvent(
                    AppEvent.ShowMessage(
                        StringValue.StringResource(R.string.error_authentication_failed)
                    )
                )
            },
            onFailure = {
                viewModel.toggleShowAuthPrompt()
                appViewModel.handleEvent(
                    AppEvent.ShowMessage(
                        StringValue.StringResource(R.string.error_authorization_failed)
                    )
                )
            },
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
        modifier =
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
    ) {
        InterfaceSection(isTunnelNameTaken, uiState, viewModel)
        PeersSection(uiState, viewModel)
        AddPeerButton(viewModel)
    }
}
