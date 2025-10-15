package com.zaneschepke.wireguardautotunnel.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewQuilt
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.ui.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.LocalSharedVm
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.ui.common.button.SwitchWithDivider
import com.zaneschepke.wireguardautotunnel.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.ui.common.text.DescriptionText
import com.zaneschepke.wireguardautotunnel.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.BackupBottomSheet
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.proxy.compoents.AppModeBottomSheet
import com.zaneschepke.wireguardautotunnel.util.StringValue
import com.zaneschepke.wireguardautotunnel.util.extensions.asString
import com.zaneschepke.wireguardautotunnel.util.extensions.asTitleString
import com.zaneschepke.wireguardautotunnel.util.extensions.capitalize
import com.zaneschepke.wireguardautotunnel.viewmodel.SettingsViewModel
import java.util.*

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val sharedViewModel = LocalSharedVm.current

    val locale = remember { Locale.getDefault() }

    val settingsState by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    if (settingsState.isLoading) return

    var showBackupSheet by rememberSaveable { mutableStateOf(false) }
    var showAppModeSheet by rememberSaveable { mutableStateOf(false) }

    val appMode by
        rememberSaveable(settingsState.settings.appMode) {
            mutableStateOf(settingsState.settings.appMode)
        }
    val monitoringEnabled by
        rememberSaveable(appMode) {
            mutableStateOf(appMode == AppMode.VPN || appMode == AppMode.KERNEL)
        }
    val dnsEnabled by rememberSaveable(appMode) { mutableStateOf(appMode != AppMode.KERNEL) }

    val showProxySettings by
        remember(settingsState.settings.appMode) {
            derivedStateOf {
                when (settingsState.settings.appMode) {
                    AppMode.PROXY -> true
                    else -> false
                }
            }
        }

    if (showBackupSheet) BackupBottomSheet { showBackupSheet = false }
    if (showAppModeSheet)
        AppModeBottomSheet(sharedViewModel::setAppMode, settingsState.settings.appMode) {
            showAppModeSheet = false
        }

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
        modifier = Modifier.verticalScroll(rememberScrollState()).fillMaxSize(),
    ) {
        Column {
            GroupLabel(
                stringResource(R.string.tunnel).capitalize(locale),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            SurfaceRow(
                leading = {
                    Icon(ImageVector.vectorResource(R.drawable.sdk), contentDescription = null)
                },
                trailing = {
                    Icon(
                        Icons.Outlined.ExpandMore,
                        contentDescription = stringResource(R.string.select),
                    )
                },
                title = stringResource(R.string.backend_mode),
                description = {
                    DescriptionText(
                        stringResource(R.string.current_template, appMode.asTitleString(context))
                    )
                },
                onClick = { showAppModeSheet = true },
            )
            if (appMode == AppMode.LOCK_DOWN) {
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
                            checked = settingsState.settings.isLanOnKillSwitchEnabled,
                            onClick = { viewModel.setLanKillSwitchEnabled(it) },
                        )
                    },
                    onClick = {
                        viewModel.setLanKillSwitchEnabled(
                            !settingsState.settings.isLanOnKillSwitchEnabled
                        )
                    },
                )
            }

            SurfaceRow(
                leading = { Icon(Icons.Outlined.MonitorHeart, null) },
                title = stringResource(R.string.tunnel_monitoring),
                enabled = monitoringEnabled,
                onClick = { navController.push(Route.TunnelMonitoring) },
            )
            SurfaceRow(
                leading = { Icon(Icons.Outlined.Dns, null) },
                title = stringResource(R.string.dns_settings),
                enabled = dnsEnabled,
                onClick = {
                    if (dnsEnabled) navController.push(Route.Dns)
                    else
                        sharedViewModel.showSnackMessage(
                            StringValue.StringResource(
                                R.string.mode_disabled_template,
                                appMode.asString(context),
                            )
                        )
                },
            )
            SurfaceRow(
                leading = {
                    Icon(ImageVector.vectorResource(R.drawable.globe), contentDescription = null)
                },
                title = stringResource(R.string.global_overrides),
                trailing = { modifier ->
                    SwitchWithDivider(
                        checked = settingsState.settings.isTunnelGlobalsEnabled,
                        onClick = { viewModel.setTunnelGlobals(it) },
                        modifier = modifier,
                    )
                },
                onClick = {
                    settingsState.globalTunnelConf?.let {
                        navController.push(Route.TunnelGlobals(it.id))
                    }
                },
            )
            if (showProxySettings) {
                SurfaceRow(
                    leading = { Icon(ImageVector.vectorResource(R.drawable.proxy), null) },
                    title = stringResource(R.string.proxy_settings),
                    onClick = { navController.push(Route.ProxySettings) },
                )
            }
        }
        Column {
            GroupLabel(
                stringResource(R.string.general),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            SurfaceRow(
                leading = { Icon(Icons.Outlined.Android, null) },
                title = stringResource(R.string.android_integrations),
                onClick = { navController.push(Route.AndroidIntegrations) },
            )
            SurfaceRow(
                leading = {
                    Icon(Icons.AutoMirrored.Outlined.ViewQuilt, contentDescription = null)
                },
                title = stringResource(R.string.appearance),
                onClick = { navController.push(Route.Appearance) },
            )
            SurfaceRow(
                leading = { Icon(Icons.Outlined.Pin, contentDescription = null) },
                title = stringResource(R.string.enable_app_lock),
                trailing = {
                    ScaledSwitch(
                        checked = settingsState.isPinLockEnabled,
                        onClick = {
                            if (it) {
                                navController.push(Route.Lock)
                            } else {
                                sharedViewModel.setPinLockEnabled(false)
                            }
                        },
                    )
                },
                onClick = {
                    if (!settingsState.isPinLockEnabled) {
                        navController.push(Route.Lock)
                    } else {
                        sharedViewModel.setPinLockEnabled(false)
                    }
                },
            )
            SurfaceRow(
                leading = { Icon(Icons.Outlined.SettingsBackupRestore, contentDescription = null) },
                title = stringResource(R.string.backup_and_restore),
                onClick = { showBackupSheet = true },
                trailing = {
                    Icon(
                        Icons.Outlined.ExpandMore,
                        contentDescription = stringResource(R.string.select),
                    )
                },
            )
        }
    }
}
