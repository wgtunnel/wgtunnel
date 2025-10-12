package com.zaneschepke.wireguardautotunnel.ui.screens.settings.system

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.VpnLock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.SecureScreenFromRecording
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.ui.common.functions.rememberClipboardHelper
import com.zaneschepke.wireguardautotunnel.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.util.extensions.launchVpnSettings
import com.zaneschepke.wireguardautotunnel.viewmodel.SettingsViewModel

@Composable
fun AndroidIntegrationsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current

    val settingsState by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    val clipboard = rememberClipboardHelper()

    SecureScreenFromRecording()

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        Column {
            GroupLabel(stringResource(id = R.string.vpn), Modifier.padding(horizontal = 16.dp))
            SurfaceRow(
                leading = { Icon(Icons.Outlined.AdminPanelSettings, contentDescription = null) },
                title = stringResource(R.string.native_kill_switch),
                trailing = { Icon(Icons.AutoMirrored.Outlined.Launch, null) },
                onClick = { context.launchVpnSettings() },
            )
            SurfaceRow(
                leading = { Icon(Icons.Outlined.VpnLock, contentDescription = null) },
                trailing = {
                    ScaledSwitch(
                        checked = settingsState.settings.isAlwaysOnVpnEnabled,
                        onClick = { viewModel.setAlwaysOnVpnEnabled(it) },
                    )
                },
                title = stringResource(R.string.always_on_vpn_support),
                onClick = {
                    viewModel.setAlwaysOnVpnEnabled(!settingsState.settings.isAlwaysOnVpnEnabled)
                },
            )
        }
        Column {
            GroupLabel(
                stringResource(id = R.string.tunnel_control),
                Modifier.padding(horizontal = 16.dp),
            )
            SurfaceRow(
                leading = { Icon(Icons.Outlined.Restore, contentDescription = null) },
                trailing = {
                    ScaledSwitch(
                        checked = settingsState.settings.isRestoreOnBootEnabled,
                        onClick = { viewModel.setRestoreOnBootEnabled(it) },
                    )
                },
                title = stringResource(R.string.restart_at_boot),
                onClick = {
                    viewModel.setRestoreOnBootEnabled(
                        !settingsState.settings.isRestoreOnBootEnabled
                    )
                },
            )
            SurfaceRow(
                leading = { Icon(Icons.Filled.AppShortcut, contentDescription = null) },
                trailing = {
                    ScaledSwitch(
                        checked = settingsState.settings.isShortcutsEnabled,
                        onClick = { viewModel.setShortcutsEnabled(it) },
                    )
                },
                title = stringResource(R.string.enabled_app_shortcuts),
                onClick = {
                    viewModel.setShortcutsEnabled(!settingsState.settings.isShortcutsEnabled)
                },
            )
            SurfaceRow(
                leading = { Icon(Icons.Filled.SmartToy, contentDescription = null) },
                trailing = {
                    ScaledSwitch(
                        checked = settingsState.isRemoteEnabled,
                        onClick = { viewModel.setRemoteEnabled(it) },
                    )
                },
                description = {
                    settingsState.remoteKey?.let { key ->
                        AnimatedVisibility(visible = settingsState.isRemoteEnabled) {
                            Text(
                                text = stringResource(R.string.remote_key_template, key),
                                style =
                                    MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.outline
                                    ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable { clipboard.copy(key) },
                            )
                        }
                    }
                },
                title = stringResource(R.string.enable_remote_app_control),
                onClick = { viewModel.setRemoteEnabled(!settingsState.isRemoteEnabled) },
            )
        }
    }
}
