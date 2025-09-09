package com.zaneschepke.wireguardautotunnel.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.ui.LocalIsAndroidTV
import com.zaneschepke.wireguardautotunnel.ui.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.LocalSharedVm
import com.zaneschepke.wireguardautotunnel.ui.common.SectionDivider
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.*
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.proxy.compoents.AppModeBottomSheet
import com.zaneschepke.wireguardautotunnel.ui.sideeffect.LocalSideEffect
import com.zaneschepke.wireguardautotunnel.util.StringValue
import com.zaneschepke.wireguardautotunnel.util.extensions.asString
import com.zaneschepke.wireguardautotunnel.viewmodel.SettingsViewModel
import org.orbitmvi.orbit.compose.collectSideEffect
import xyz.teamgravity.pin_lock_compose.PinManager

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val isTv = LocalIsAndroidTV.current
    val context = LocalContext.current
    val navController = LocalNavController.current
    val focusManager = LocalFocusManager.current
    val sharedViewModel = LocalSharedVm.current
    val interactionSource = remember { MutableInteractionSource() }

    val settingsState by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    var showBackupSheet by rememberSaveable { mutableStateOf(false) }
    var showAppModeSheet by rememberSaveable { mutableStateOf(false) }

    val appMode by
        rememberSaveable(settingsState.settings.appMode) {
            mutableStateOf(settingsState.settings.appMode)
        }

    if (!settingsState.stateInitialized) return

    sharedViewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            LocalSideEffect.Sheet.BackupApp -> showBackupSheet = true
            else -> Unit
        }
    }

    val showProxySettings by
        remember(settingsState.settings.appMode) {
            derivedStateOf {
                when (settingsState.settings.appMode) {
                    AppMode.PROXY -> true
                    else -> false
                }
            }
        }

    if (showBackupSheet) BackupBottomSheet() { showBackupSheet = false }
    if (showAppModeSheet)
        AppModeBottomSheet(sharedViewModel::setAppMode, settingsState.settings.appMode) {
            showAppModeSheet = false
        }

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
        modifier =
            Modifier.verticalScroll(rememberScrollState())
                .fillMaxSize()
                .padding(vertical = 24.dp)
                .padding(horizontal = 12.dp)
                .then(
                    if (!isTv) {
                        Modifier.clickable(
                            indication = null,
                            interactionSource = interactionSource,
                            onClick = { focusManager.clearFocus() },
                        )
                    } else {
                        Modifier
                    }
                ),
    ) {
        SurfaceSelectionGroupButton(
            listOf(appModeItem(settingsState.settings.appMode) { showAppModeSheet = true })
        )
        SectionDivider()
        SurfaceSelectionGroupButton(
            items =
                buildList {
                    if (appMode == AppMode.LOCK_DOWN) {
                        add(
                            lanTrafficItem(settingsState.settings.isLanOnKillSwitchEnabled) {
                                viewModel.setLanKillSwitchEnabled(it)
                            }
                        )
                    }
                    add(
                        tunnelMonitoringItem(
                            appMode,
                            onClick = { navController.navigate(Route.TunnelMonitoring) },
                        ) {
                            sharedViewModel.showSnackMessage(
                                StringValue.StringResource(
                                    R.string.mode_disabled_template,
                                    appMode.asString(context),
                                )
                            )
                        }
                    )
                    add(
                        dnsSettingsItem(appMode, onClick = { navController.navigate(Route.Dns) }) {
                            sharedViewModel.showSnackMessage(
                                StringValue.StringResource(
                                    R.string.mode_disabled_template,
                                    appMode.asString(context),
                                )
                            )
                        }
                    )
                    if (showProxySettings)
                        add(proxYSettingsItem() { navController.navigate(Route.ProxySettings) })
                }
        )
        SectionDivider()
        SurfaceSelectionGroupButton(
            listOf(systemFeaturesItem() { navController.navigate(Route.SystemFeatures) })
        )
        SectionDivider()
        SurfaceSelectionGroupButton(
            items =
                buildList {
                    add(appearanceItem() { navController.navigate(Route.Appearance) })
                    add(
                        localLoggingItem(settingsState.isLocalLoggingEnabled) {
                            viewModel.setLocalLogging(it)
                        }
                    )
                    if (settingsState.isLocalLoggingEnabled)
                        add(readLogsItem() { navController.navigate(Route.Logs) })
                    add(
                        pinLockItem(settingsState.isPinLockEnabled) { enabled ->
                            if (enabled) {
                                PinManager.initialize(context)
                                navController.navigate(Route.Lock)
                            } else {
                                sharedViewModel.setPinLockEnabled(false)
                            }
                        }
                    )
                }
        )
    }
}
