package com.zaneschepke.wireguardautotunnel.ui.screens.settings.globals

import android.R.attr.onClick
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.ui.navigation.Route

@Composable
fun TunnelGlobalsScreen(globalTunnelId: Int) {
    val navController = LocalNavController.current

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
        modifier = Modifier.verticalScroll(rememberScrollState()).fillMaxSize(),
    ) {
        Column {
            SurfaceRow(
                leading = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                title = stringResource(R.string.configuration),
                onClick = { navController.push(Route.ConfigGlobal(globalTunnelId)) },
            )
            SurfaceRow(
                leading = {
                    Icon(Icons.AutoMirrored.Outlined.CallSplit, contentDescription = null)
                },
                title = stringResource(R.string.splt_tunneling),
                onClick = { navController.push(Route.SplitTunnelGlobal(id = globalTunnelId)) },
            )
        }
    }
}
