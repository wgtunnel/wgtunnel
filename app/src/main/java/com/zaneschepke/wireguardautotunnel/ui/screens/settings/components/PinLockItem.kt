package com.zaneschepke.wireguardautotunnel.ui.screens.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent
import xyz.teamgravity.pin_lock_compose.PinManager

@Composable
fun PinLockItem(uiState: AppUiState, viewModel: AppViewModel): SelectionItem {
    val navController = LocalNavController.current
    val context = LocalContext.current

    fun onPinLockToggle() {
        if (uiState.appState.isPinLockEnabled) {
            viewModel.handleEvent(AppEvent.TogglePinLock)
        } else {
            PinManager.initialize(context)
            navController.navigate(Route.Lock)
        }
    }

    return SelectionItem(
        leading = { Icon(Icons.Outlined.Pin, contentDescription = null) },
        title = {
            Text(
                text = stringResource(R.string.enable_app_lock),
                style =
                    MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
            )
        },
        trailing = {
            ScaledSwitch(
                checked = uiState.appState.isPinLockEnabled,
                onClick = { onPinLockToggle() },
            )
        },
        onClick = { onPinLockToggle() },
    )
}
