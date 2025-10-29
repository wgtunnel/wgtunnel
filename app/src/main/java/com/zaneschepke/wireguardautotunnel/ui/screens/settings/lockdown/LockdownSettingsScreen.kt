package com.zaneschepke.wireguardautotunnel.ui.screens.settings.lockdown

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.viewmodel.LockdownViewModel

@Composable
fun LockdownSettingsScreen(viewModel: LockdownViewModel = hiltViewModel()) {

    val uiState by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    if (uiState.isLoading) return

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        Column {
            GroupLabel(
                stringResource(R.string.configuration),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            SurfaceRow(
                leading = { Icon(Icons.Outlined.Lan, contentDescription = null) },
                title = stringResource(R.string.allow_lan_traffic),
                description = {
                    Text(
                        text = stringResource(R.string.bypass_lan_for_kill_switch),
                        style =
                            MaterialTheme.typography.bodySmall.copy(
                                MaterialTheme.colorScheme.outline
                            ),
                    )
                },
                trailing = {
                    ScaledSwitch(
                        checked = uiState.lockdownSettings.bypassLan,
                        onClick = { viewModel.setBypassLan(it) },
                    )
                },
                onClick = { viewModel.setBypassLan(!uiState.lockdownSettings.bypassLan) },
            )
            SurfaceRow(
                leading = { Icon(Icons.Outlined.DataUsage, contentDescription = null) },
                title = stringResource(R.string.metered_tunnel),
                trailing = {
                    ScaledSwitch(
                        checked = uiState.lockdownSettings.metered,
                        onClick = { viewModel.setMetered(it) },
                    )
                },
                onClick = { viewModel.setMetered(!uiState.lockdownSettings.metered) },
            )
            SurfaceRow(
                leading = {
                    Icon(ImageVector.vectorResource(R.drawable.host), contentDescription = null)
                },
                title = "Dual-stack",
                trailing = {
                    ScaledSwitch(
                        checked = uiState.lockdownSettings.dualStack,
                        onClick = { viewModel.setDualStack(it) },
                    )
                },
                onClick = { viewModel.setDualStack(!uiState.lockdownSettings.dualStack) },
            )
        }
    }
}
