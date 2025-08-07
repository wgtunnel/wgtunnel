package com.zaneschepke.wireguardautotunnel.ui.screens.settings.monitoring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TimerOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.common.dropdown.LabelledNumberDropdown
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.monitoring.components.detailedPingStatsItem
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.monitoring.components.enablePingMonitoringItem
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun TunnelMonitoringScreen(uiState: AppUiState, viewModel: AppViewModel) {
    val pingInterval: Int by
        remember(uiState.appSettings) {
            mutableIntStateOf(uiState.appSettings.tunnelPingIntervalSeconds)
        }
    val pingAttempts: Int by
        remember(uiState.appSettings) { mutableIntStateOf(uiState.appSettings.tunnelPingAttempts) }
    val pingTimeout: Int? by
        remember(uiState.appSettings) {
            mutableStateOf(uiState.appSettings.tunnelPingTimeoutSeconds)
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
        SurfaceSelectionGroupButton(listOf(enablePingMonitoringItem(uiState, viewModel)))
        if (uiState.appSettings.isPingEnabled) {
            LabelledNumberDropdown(
                title = {
                    Text(
                        text = stringResource(R.string.tunnel_ping_interval),
                        style =
                            MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                    )
                },
                leading = { Icon(Icons.Outlined.Timer, contentDescription = null) },
                currentValue = pingInterval,
                onSelected = { selected ->
                    viewModel.handleEvent(AppEvent.SetPingInterval(selected!!))
                },
                options = (10..60).step(10).toList(),
            )
            LabelledNumberDropdown(
                title = {
                    Text(
                        text = stringResource(R.string.attempts_per_interval),
                        style =
                            MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                    )
                },
                leading = { Icon(Icons.Outlined.Replay, contentDescription = null) },
                currentValue = pingAttempts,
                onSelected = { selected ->
                    viewModel.handleEvent(AppEvent.SetPingAttempts(selected!!))
                },
                options = (1..5).toList(),
            )
            LabelledNumberDropdown(
                title = {
                    Text(
                        text = stringResource(R.string.ping_timeout),
                        style =
                            MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                    )
                },
                leading = { Icon(Icons.Outlined.TimerOff, contentDescription = null) },
                currentValue = pingTimeout,
                description = {
                    Text(
                        text = stringResource(R.string.timeout_all_attempts),
                        style =
                            MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.outline
                            ),
                    )
                },
                onSelected = { selected ->
                    viewModel.handleEvent(AppEvent.SetPingTimeout(selected))
                },
                options = (10..20).toList() + null,
            )
            SurfaceSelectionGroupButton(listOf(detailedPingStatsItem(uiState, viewModel)))
        }
    }
}
