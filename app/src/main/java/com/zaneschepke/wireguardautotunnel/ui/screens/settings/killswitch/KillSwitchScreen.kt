package com.zaneschepke.wireguardautotunnel.ui.screens.settings.killswitch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendMode
import com.zaneschepke.wireguardautotunnel.ui.common.SectionDivider
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.navigation.LocalIsAndroidTV
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.killswitch.components.LanTrafficItem
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.killswitch.components.VpnKillSwitchItem
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.killswitch.components.nativeKillSwitchItem
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun KillSwitchScreen(uiState: AppUiState, viewModel: AppViewModel) {
    val isTv = LocalIsAndroidTV.current

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
        modifier = Modifier.fillMaxSize().padding(vertical = 24.dp).padding(horizontal = 12.dp),
    ) {
        if (!isTv) {
            SurfaceSelectionGroupButton(items = listOf(nativeKillSwitchItem()))
            SectionDivider()
        }
        SurfaceSelectionGroupButton(
            items =
                buildList {
                    if (uiState.appSettings.backendMode != BackendMode.KERNEL) {
                        add(
                            VpnKillSwitchItem(uiState) {
                                viewModel.handleEvent(AppEvent.ToggleVpnKillSwitch)
                            }
                        )
                        if (uiState.appSettings.isVpnKillSwitchEnabled) {
                            add(LanTrafficItem(uiState, viewModel))
                        }
                    }
                }
        )
    }
}
