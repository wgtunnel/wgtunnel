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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.LocalSharedVm
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.common.dropdown.LabelledDropdown
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.monitoring.components.detailedPingStatsItem
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.monitoring.components.enablePingMonitoringItem
import com.zaneschepke.wireguardautotunnel.ui.state.NavbarState
import com.zaneschepke.wireguardautotunnel.viewmodel.SettingsViewModel

@Composable
fun TunnelMonitoringScreen(viewModel: SettingsViewModel) {
    val sharedViewModel = LocalSharedVm.current
    val settingsState by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        sharedViewModel.updateNavbarState(
            NavbarState(
                topTitle = { Text(stringResource(R.string.tunnel_monitoring)) },
                showBottomItems = true,
            )
        )
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
        SurfaceSelectionGroupButton(
            listOf(
                enablePingMonitoringItem(settingsState.settings.isPingEnabled) {
                    viewModel.setPingEnabled(it)
                }
            )
        )
        if (settingsState.settings.isPingEnabled) {
            LabelledDropdown(
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
                currentValue = settingsState.settings.tunnelPingIntervalSeconds,
                onSelected = { selected ->
                    selected?.let { viewModel.setTunnelPingIntervalSeconds(it) }
                },
                options = (10..60).step(10).toList(),
                optionToString = { it?.toString() ?: stringResource(R.string._default) },
            )
            LabelledDropdown(
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
                currentValue = settingsState.settings.tunnelPingAttempts,
                onSelected = { selected -> selected?.let { viewModel.setTunnelPingAttempts(it) } },
                options = (1..5).toList(),
                optionToString = { it?.toString() ?: stringResource(R.string._default) },
            )
            LabelledDropdown(
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
                currentValue = settingsState.settings.tunnelPingTimeoutSeconds,
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
                    selected?.let { viewModel.setTunnelPingTimeoutSeconds(it) }
                },
                options = (10..20).toList() + null,
                optionToString = { it?.toString() ?: stringResource(R.string._default) },
            )
            SurfaceSelectionGroupButton(
                listOf(
                    detailedPingStatsItem(settingsState.showDetailedPingStats) {
                        viewModel.setDetailedPingStats(it)
                    }
                )
            )
        }
    }
}
