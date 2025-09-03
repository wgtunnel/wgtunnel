package com.zaneschepke.wireguardautotunnel.ui.screens.settings.system

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.ui.LocalIsAndroidTV
import com.zaneschepke.wireguardautotunnel.ui.common.SectionDivider
import com.zaneschepke.wireguardautotunnel.ui.common.SecureScreenFromRecording
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.system.components.*
import com.zaneschepke.wireguardautotunnel.viewmodel.SettingsViewModel

@Composable
fun SystemFeaturesScreen(viewModel: SettingsViewModel) {
    val settingsState by viewModel.container.stateFlow.collectAsStateWithLifecycle()

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
                add(
                    restartAtBootItem(settingsState.settings.isRestoreOnBootEnabled) {
                        viewModel.setRestoreOnBootEnabled(it)
                    }
                )
                if (!isTv)
                    add(
                        alwaysOnVpnItem(settingsState.settings.isAlwaysOnVpnEnabled) {
                            viewModel.setAlwaysOnVpnEnabled(it)
                        }
                    )
            }
        )
        SectionDivider()
        SurfaceSelectionGroupButton(
            buildList {
                add(
                    appShortcutsItem(settingsState.settings.isShortcutsEnabled) {
                        viewModel.setShortcutsEnabled(it)
                    }
                )
                add(
                    remoteControlItem(settingsState.isRemoteEnabled, settingsState.remoteKey) {
                        viewModel.setRemoteEnabled(it)
                    }
                )
            }
        )
    }
}
