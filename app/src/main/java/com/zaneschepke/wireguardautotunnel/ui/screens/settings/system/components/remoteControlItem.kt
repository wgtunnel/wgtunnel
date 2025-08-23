package com.zaneschepke.wireguardautotunnel.ui.screens.settings.system.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.functions.rememberClipboardHelper
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun remoteControlItem(uiState: AppUiState, viewModel: AppViewModel): SelectionItem {
    val clipboardManager = rememberClipboardHelper()

    return SelectionItem(
        leading = { Icon(Icons.Filled.SmartToy, contentDescription = null) },
        trailing = {
            ScaledSwitch(
                checked = uiState.appState.isRemoteControlEnabled,
                onClick = { viewModel.handleEvent(AppEvent.ToggleRemoteControl) },
            )
        },
        description = {
            uiState.appState.remoteKey?.let { key ->
                AnimatedVisibility(visible = uiState.appState.isRemoteControlEnabled) {
                    Text(
                        text = stringResource(R.string.remote_key_template, key),
                        style =
                            MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.outline
                            ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { clipboardManager.copy(key) },
                    )
                }
            }
        },
        title = {
            Text(
                text = stringResource(R.string.enable_remote_app_control),
                style =
                    MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
            )
        },
        onClick = { viewModel.handleEvent(AppEvent.ToggleRemoteControl) },
    )
}
