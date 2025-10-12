package com.zaneschepke.wireguardautotunnel.ui.screens.settings.monitoring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NetworkPing
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TimerOff
import androidx.compose.material.icons.outlined.ViewHeadline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.ui.common.button.SwitchWithDivider
import com.zaneschepke.wireguardautotunnel.ui.common.dropdown.LabelledDropdown
import com.zaneschepke.wireguardautotunnel.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.viewmodel.SettingsViewModel

@Composable
fun TunnelMonitoringScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val navController = LocalNavController.current
    val settingsState by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    if (settingsState.isLoading) return

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        Column {
            GroupLabel(
                stringResource(R.string.ping),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            SurfaceRow(
                leading = { Icon(Icons.Outlined.NetworkPing, contentDescription = null) },
                title = stringResource(R.string.ping_monitor),
                trailing = {
                    ScaledSwitch(
                        checked = settingsState.settings.isPingEnabled,
                        onClick = { viewModel.setPingEnabled(it) },
                    )
                },
                onClick = { viewModel.setPingEnabled(!settingsState.settings.isPingEnabled) },
            )
            LabelledDropdown(
                title = stringResource(R.string.tunnel_ping_interval),
                leading = { Icon(Icons.Outlined.Timer, contentDescription = null) },
                currentValue = settingsState.settings.tunnelPingIntervalSeconds,
                onSelected = { selected ->
                    selected?.let { viewModel.setTunnelPingIntervalSeconds(it) }
                },
                options = (10..60).step(10).toList(),
                optionToString = { it?.toString() ?: stringResource(R.string._default) },
            )
            LabelledDropdown(
                title = stringResource(R.string.attempts_per_interval),
                leading = { Icon(Icons.Outlined.Replay, contentDescription = null) },
                currentValue = settingsState.settings.tunnelPingAttempts,
                onSelected = { selected -> selected?.let { viewModel.setTunnelPingAttempts(it) } },
                options = (1..5).toList(),
                optionToString = { it?.toString() ?: stringResource(R.string._default) },
            )
            LabelledDropdown(
                title = stringResource(R.string.ping_timeout),
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
            SurfaceRow(
                leading = { Icon(Icons.Outlined.QueryStats, contentDescription = null) },
                title = stringResource(R.string.display_detailed_ping_stats),
                trailing = {
                    ScaledSwitch(
                        checked = settingsState.showDetailedPingStats,
                        onClick = { viewModel.setDetailedPingStats(it) },
                    )
                },
                onClick = { viewModel.setDetailedPingStats(!settingsState.showDetailedPingStats) },
            )
        }
        Column {
            GroupLabel(
                stringResource(R.string.other),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            SurfaceRow(
                leading = { Icon(Icons.Outlined.ViewHeadline, contentDescription = null) },
                title = stringResource(R.string.local_logging),
                trailing = { modifier ->
                    SwitchWithDivider(
                        checked = settingsState.isLocalLoggingEnabled,
                        onClick = { viewModel.setLocalLogging(it) },
                        modifier = modifier,
                    )
                },
                onClick = { navController.push(Route.Logs) },
            )
        }
    }
}
