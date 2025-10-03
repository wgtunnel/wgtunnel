package com.zaneschepke.wireguardautotunnel.ui.screens.settings.globals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.globals.components.globalConfigItem
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.tunneloptions.components.splitTunnelingItem

@Composable
fun TunnelGlobalsScreen(globalTunnelId: Int) {
    val navController = LocalNavController.current

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
        modifier =
            Modifier.verticalScroll(rememberScrollState()).fillMaxSize().padding(horizontal = 16.dp),
    ) {
        SurfaceSelectionGroupButton(
            listOf(
                globalConfigItem { navController.push(Route.ConfigGlobal(globalTunnelId)) },
                splitTunnelingItem(stringResource(R.string.splt_tunneling)) {
                    navController.push(Route.SplitTunnelGlobal(id = globalTunnelId))
                },
            )
        )
    }
}
