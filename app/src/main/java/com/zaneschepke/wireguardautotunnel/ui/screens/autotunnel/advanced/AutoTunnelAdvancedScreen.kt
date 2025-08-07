package com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.advanced

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.dropdown.LabelledNumberDropdown
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun AutoTunnelAdvancedScreen(appUiState: AppUiState, viewModel: AppViewModel) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
        modifier =
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp)
                .padding(horizontal = 12.dp),
    ) {
        LabelledNumberDropdown(
            title = {
                Text(
                    stringResource(R.string.debounce_delay),
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                )
            },
            leading = { Icon(Icons.Outlined.PauseCircle, null) },
            onSelected = { selected ->
                viewModel.handleEvent(AppEvent.SetDebounceDelay(selected!!))
            },
            options = (0..10).toList(),
            currentValue = appUiState.appSettings.debounceDelaySeconds,
        )
    }
}
