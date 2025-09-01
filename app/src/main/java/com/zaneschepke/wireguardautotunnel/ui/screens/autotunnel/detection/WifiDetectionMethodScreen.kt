package com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.detection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.model.WifiDetectionMethod
import com.zaneschepke.wireguardautotunnel.ui.LocalSharedVm
import com.zaneschepke.wireguardautotunnel.ui.common.button.IconSurfaceButton
import com.zaneschepke.wireguardautotunnel.ui.state.NavbarState
import com.zaneschepke.wireguardautotunnel.util.extensions.asDescriptionString
import com.zaneschepke.wireguardautotunnel.util.extensions.asTitleString
import com.zaneschepke.wireguardautotunnel.viewmodel.AutoTunnelViewModel

@Composable
fun WifiDetectionMethodScreen(viewModel: AutoTunnelViewModel) {
    val context = LocalContext.current
    val sharedViewModel = LocalSharedVm.current

    val autoTunnelState by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        sharedViewModel.updateNavbarState(
            NavbarState(
                topTitle = { Text(stringResource(R.string.wifi_detection_method)) },
                showBottomItems = true,
            )
        )
    }

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
        modifier = Modifier.fillMaxSize().padding(top = 24.dp).padding(horizontal = 24.dp),
    ) {
        enumValues<WifiDetectionMethod>().forEach {
            val title = it.asTitleString(context)
            val description = it.asDescriptionString(context)
            IconSurfaceButton(
                title = title,
                onClick = { sharedViewModel.setWifiDetectionMethod(it) },
                selected = autoTunnelState.generalSettings.wifiDetectionMethod == it,
                description = description,
            )
        }
    }
}
