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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.SectionDivider
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.navigation.LocalIsAndroidTV
import com.zaneschepke.wireguardautotunnel.ui.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.components.AdvancedSettingsItem
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.AppShortcutsItem
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.KernelModeItem
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.LocalLoggingItem
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.PinLockItem
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.ReadLogsItem
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.RestartAtBootItem
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.alwaysOnVpnItem
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.appearanceItem
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.killSwitchItem
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel

@Composable
fun SettingsScreen(uiState: AppUiState, viewModel: AppViewModel) {
    val isTv = LocalIsAndroidTV.current
    val focusManager = LocalFocusManager.current
    val navController = LocalNavController.current

    val interactionSource = remember { MutableInteractionSource() }

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
            items =
                buildList {
                    add(AppShortcutsItem(uiState, viewModel))
                    if (!isTv) add(alwaysOnVpnItem(uiState, viewModel))
                    add(killSwitchItem())
                    add(RestartAtBootItem(uiState, viewModel))
                }
        )
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
        SectionDivider()
        if (!isTv) {
            SurfaceSelectionGroupButton(items = listOf(KernelModeItem(uiState, viewModel)))
            SectionDivider()
        }
        SurfaceSelectionGroupButton(
            items =
                listOf(
                    AdvancedSettingsItem(
                        onClick = { navController.navigate(Route.SettingsAdvanced) }
                    )
                )
        )
    }
}
