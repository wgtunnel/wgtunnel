package com.zaneschepke.wireguardautotunnel.ui.screens.settings.integrations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.ui.common.functions.rememberClipboardHelper
import com.zaneschepke.wireguardautotunnel.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.ui.common.security.SecureScreenFromRecording
import com.zaneschepke.wireguardautotunnel.ui.common.text.DescriptionText
import com.zaneschepke.wireguardautotunnel.util.extensions.launchVpnSettings
import com.zaneschepke.wireguardautotunnel.viewmodel.SettingsViewModel

@Composable
fun AndroidIntegrationsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current

    val settingsState by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    if (settingsState.isLoading) return

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
                description = { DescriptionText(stringResource(R.string.aovpn_description)) },
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
                description = { DescriptionText(stringResource(R.string.tunnel_boot_description)) },
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
                title = stringResource(R.string.enable_remote_app_control),
                onClick = { viewModel.setRemoteEnabled(!settingsState.isRemoteEnabled) },
            )
            AnimatedVisibility(settingsState.isRemoteEnabled) {
                settingsState.remoteKey?.let { key ->
                    var passwordProtected by remember { mutableStateOf(true) }
                    val keyText by
                        remember(passwordProtected) {
                            derivedStateOf {
                                if (passwordProtected) "•".repeat(key.length) else key
                            }
                        }
                    SurfaceRow(
                        leading = { Icon(Icons.Outlined.Key, contentDescription = null) },
                        title = stringResource(R.string.remote_key),
                        description = {
                            Text(
                                text = keyText,
                                style =
                                    MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.outline
                                    ),
                                overflow = TextOverflow.Clip,
                            )
                        },
                        trailing = {
                            Row {
                                IconButton(onClick = { passwordProtected = !passwordProtected }) {
                                    Icon(
                                        Icons.Outlined.RemoveRedEye,
                                        contentDescription = stringResource(R.string.show_password),
                                    )
                                }
                                IconButton(onClick = { clipboard.copy(key) }) {
                                    Icon(
                                        Icons.Outlined.ContentCopy,
                                        contentDescription = stringResource(R.string.copy),
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}
