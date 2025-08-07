package com.zaneschepke.wireguardautotunnel.ui.screens.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun appShortcutsItem(uiState: AppUiState, viewModel: AppViewModel): SelectionItem {
    return SelectionItem(
        leading = { Icon(Icons.Filled.AppShortcut, contentDescription = null) },
        trailing = {
            ScaledSwitch(
                checked = uiState.appSettings.isShortcutsEnabled,
                onClick = { viewModel.handleEvent(AppEvent.ToggleAppShortcuts) },
            )
        },
        title = {
            Text(
                text = stringResource(R.string.enabled_app_shortcuts),
                style =
                    MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
            )
        },
        onClick = { viewModel.handleEvent(AppEvent.ToggleAppShortcuts) },
    )
}
