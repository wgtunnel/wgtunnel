package com.zaneschepke.wireguardautotunnel.ui.screens.settings.system

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.ui.common.SectionDivider
import com.zaneschepke.wireguardautotunnel.ui.common.SecureScreenFromRecording
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.navigation.LocalIsAndroidTV
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.system.components.*
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel

@Composable
fun SystemFeaturesScreen(appUiState: AppUiState, viewModel: AppViewModel) {

    val isTv = LocalIsAndroidTV.current

    SecureScreenFromRecording()

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
        modifier =
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp)
                .padding(horizontal = 12.dp),
    ) {
        SurfaceSelectionGroupButton(buildList { if (!isTv) add(nativeKillSwitchItem()) })
        SectionDivider()
        SurfaceSelectionGroupButton(
            buildList {
                add(restartAtBootItem(appUiState, viewModel))
                if (!isTv) add(alwaysOnVpnItem(appUiState, viewModel))
            }
        )
        SectionDivider()
        SurfaceSelectionGroupButton(
            buildList {
                add(appShortcutsItem(appUiState, viewModel))
                add(remoteControlItem(appUiState, viewModel))
            }
        )
    }
}
