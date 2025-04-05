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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.AlwaysOnVpnItem
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.AppShortcutsItem
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.AppearanceItem
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.ExportTunnelsBottomSheet
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.KernelModeItem
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.KillSwitchItem
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.LocalLoggingItem
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.PinLockItem
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.ReadLogsItem
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.RestartAtBootItem
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.ui.state.AppViewState
import com.zaneschepke.wireguardautotunnel.util.extensions.isRunningOnTv

import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel

@Composable
fun SettingsScreen(uiState: AppUiState, appViewState: AppViewState, viewModel: AppViewModel) {
	val context = LocalContext.current
	val focusManager = LocalFocusManager.current
	val isRunningOnTv = remember { context.isRunningOnTv() }
	val interactionSource = remember { MutableInteractionSource() }

	if (appViewState.showBottomSheet) {
		ExportTunnelsBottomSheet(viewModel, isRunningOnTv)
	}

	Column(
		horizontalAlignment = Alignment.Start,
		verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
		modifier = Modifier
			.verticalScroll(rememberScrollState())
			.fillMaxSize()
			.padding(top = 24.dp)
			.padding(bottom = 40.dp)
			.padding(horizontal = 24.dp)
			.then(
				if (!isRunningOnTv) {
					Modifier.clickable(
						indication = null,
						interactionSource = interactionSource,
						onClick = { focusManager.clearFocus() },
					)
				} else {
					Modifier
				},
			),
	) {
		SurfaceSelectionGroupButton(
			items = buildList {
				add(AppShortcutsItem(uiState, viewModel))
				if (!isRunningOnTv) add(AlwaysOnVpnItem(uiState, viewModel))
				add(KillSwitchItem())
				add(RestartAtBootItem(uiState, viewModel))
			},
		)
		SurfaceSelectionGroupButton(
			items = buildList {
				add(AppearanceItem())
				add(LocalLoggingItem(uiState, viewModel))
				if (uiState.generalState.isLocalLogsEnabled) add(ReadLogsItem())
				add(PinLockItem(uiState, viewModel))
			},
		)
		if (!isRunningOnTv) {
			SurfaceSelectionGroupButton(
				items = listOf(KernelModeItem(uiState, viewModel)),
			)
		}
	}
}
