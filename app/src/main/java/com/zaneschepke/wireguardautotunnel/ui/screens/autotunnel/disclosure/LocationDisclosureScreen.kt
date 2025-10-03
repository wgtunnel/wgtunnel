package com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.disclosure

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.zaneschepke.wireguardautotunnel.ui.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.disclosure.components.LocationDisclosureHeader
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.disclosure.components.appSettingsItem
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.disclosure.components.skipItem
import com.zaneschepke.wireguardautotunnel.viewmodel.AutoTunnelViewModel

@Composable
fun LocationDisclosureScreen(viewModel: AutoTunnelViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val navController = LocalNavController.current

    fun goToAutoTunnel() {
        navController.popUpTo(Route.AutoTunnel)
    }

    val settingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            goToAutoTunnel()
        }

    LaunchedEffect(Unit) { viewModel.setLocationDisclosureShown() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.Top),
        modifier = Modifier.fillMaxSize().padding(top = 18.dp).padding(horizontal = 16.dp),
    ) {
        LocationDisclosureHeader()
        SurfaceSelectionGroupButton(
            items =
                listOf(
                    appSettingsItem {
                        val intent =
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                        settingsLauncher.launch(intent)
                    }
                )
        )
        SurfaceSelectionGroupButton(items = listOf(skipItem { goToAutoTunnel() }))
    }
}
