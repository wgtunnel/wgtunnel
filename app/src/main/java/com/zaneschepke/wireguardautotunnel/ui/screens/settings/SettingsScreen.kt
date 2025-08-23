package com.zaneschepke.wireguardautotunnel.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.ui.common.SectionDivider
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.navigation.LocalIsAndroidTV
import com.zaneschepke.wireguardautotunnel.ui.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.*
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.proxy.compoents.BackendModeBottomSheet
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.ui.state.AppViewState
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel

@Composable
fun SettingsScreen(uiState: AppUiState, appViewState: AppViewState, viewModel: AppViewModel) {
    val isTv = LocalIsAndroidTV.current
    val focusManager = LocalFocusManager.current
    val navController = LocalNavController.current

    val interactionSource = remember { MutableInteractionSource() }

    val showBackupBottomSheet by
        remember(appViewState.bottomSheet) {
            derivedStateOf {
                appViewState.bottomSheet == AppViewState.BottomSheet.BACKUP_AND_RESTORE
            }
        }
    val showBottomSheet by
        remember(appViewState.bottomSheet) {
            derivedStateOf { appViewState.bottomSheet == AppViewState.BottomSheet.BACKEND }
        }
    val showProxySettings by
        remember(uiState.appSettings.appMode) {
            derivedStateOf {
                when (uiState.appSettings.appMode) {
                    AppMode.PROXY -> true
                    else -> false
                }
            }
        }

    if (showBackupBottomSheet) BackupBottomSheet(viewModel)
    if (showBottomSheet) BackendModeBottomSheet(uiState.appSettings, viewModel)

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
        SurfaceSelectionGroupButton(buildList { add(backendModeItem(uiState, viewModel)) })
        SectionDivider()
        SurfaceSelectionGroupButton(
            items =
                buildList {
                    if (uiState.appSettings.appMode == AppMode.LOCK_DOWN) {
                        add(lanTrafficItem(uiState, viewModel))
                    }
                    add(tunnelMonitoringItem())
                    add(dnsSettingsItem())
                    // TODO changing these settings won't work in certain app states
                    if (showProxySettings) add(proxYSettingsItem())
                }
        )
        SectionDivider()
        SurfaceSelectionGroupButton(listOf(systemFeaturesItem()))
        SectionDivider()
        SurfaceSelectionGroupButton(
            items =
                buildList {
                    add(appearanceItem())
                    add(LocalLoggingItem(uiState, viewModel))
                    if (uiState.appState.isLocalLogsEnabled) add(ReadLogsItem())
                    add(PinLockItem(uiState, viewModel))
                }
        )
    }
}
