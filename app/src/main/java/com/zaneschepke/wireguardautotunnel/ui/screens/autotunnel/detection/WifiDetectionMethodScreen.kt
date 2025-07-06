package com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.detection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zaneschepke.networkmonitor.AndroidNetworkMonitor
import com.zaneschepke.wireguardautotunnel.ui.common.button.IconSurfaceButton
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.util.extensions.asDescriptionString
import com.zaneschepke.wireguardautotunnel.util.extensions.asString
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun WifiDetectionMethodScreen(uiState: AppUiState, viewModel: AppViewModel) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
        modifier = Modifier.fillMaxSize().padding(top = 24.dp).padding(horizontal = 24.dp),
    ) {
        enumValues<AndroidNetworkMonitor.WifiDetectionMethod>().forEach {
            val title = it.asString(context)
            val description = it.asDescriptionString(context)
            IconSurfaceButton(
                title = title,
                onClick = { viewModel.handleEvent(AppEvent.SetDetectionMethod(it)) },
                selected = uiState.appSettings.wifiDetectionMethod == it,
                description = description,
            )
        }
    }
}
