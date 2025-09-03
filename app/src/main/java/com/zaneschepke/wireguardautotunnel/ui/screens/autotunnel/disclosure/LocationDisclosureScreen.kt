package com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.disclosure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.ui.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.disclosure.components.LocationDisclosureHeader
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.disclosure.components.appSettingsItem
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.disclosure.components.skipItem
import com.zaneschepke.wireguardautotunnel.viewmodel.AutoTunnelViewModel

@Composable
fun LocationDisclosureScreen(viewModel: AutoTunnelViewModel) {
    val navController = LocalNavController.current

    LaunchedEffect(Unit) { viewModel.setLocationDisclosureShown() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.Top),
        modifier = Modifier.fillMaxSize().padding(top = 18.dp).padding(horizontal = 24.dp),
    ) {
        LocationDisclosureHeader()
        SurfaceSelectionGroupButton(items = listOf(appSettingsItem()))
        SurfaceSelectionGroupButton(items = listOf(skipItem(navController)))
    }
}
