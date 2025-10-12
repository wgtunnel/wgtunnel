package com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.preferences.wifi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Filter1
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.WifiFind
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.ui.common.text.DescriptionText
import com.zaneschepke.wireguardautotunnel.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.ui.navigation.TunnelNetwork
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.components.TrustedNetworkTextBox
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.components.WildcardsLabel
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.LearnMoreLinkLabel
import com.zaneschepke.wireguardautotunnel.util.extensions.asTitleString
import com.zaneschepke.wireguardautotunnel.util.extensions.openWebUrl
import com.zaneschepke.wireguardautotunnel.viewmodel.AutoTunnelViewModel

@Composable
fun WifiSettingsScreen(viewModel: AutoTunnelViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val navController = LocalNavController.current

    val autoTunnelState by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    var currentText by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(autoTunnelState.settings.trustedNetworkSSIDs) { currentText = "" }

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        Column {
            GroupLabel(stringResource(R.string.general), Modifier.padding(horizontal = 16.dp))
            SurfaceRow(
                leading = { Icon(Icons.Outlined.WifiFind, contentDescription = null) },
                title = stringResource(R.string.wifi_detection_method),
                description = {
                    DescriptionText(
                        stringResource(
                            R.string.current_template,
                            autoTunnelState.settings.wifiDetectionMethod.asTitleString(context),
                        )
                    )
                },
                onClick = { navController.push(Route.WifiDetectionMethod) },
            )
            SurfaceRow(
                leading = { Icon(Icons.Outlined.Filter1, contentDescription = null) },
                title = stringResource(R.string.use_wildcards),
                description = {
                    LearnMoreLinkLabel(
                        { context.openWebUrl(it) },
                        stringResource(R.string.docs_wildcards),
                    )
                },
                trailing = {
                    ScaledSwitch(
                        checked = autoTunnelState.settings.isWildcardsEnabled,
                        onClick = { viewModel.setWildcardsEnabled(it) },
                    )
                },
                onClick = {
                    viewModel.setWildcardsEnabled(!autoTunnelState.settings.isWildcardsEnabled)
                },
            )
        }
        Column {
            GroupLabel(stringResource(R.string.networks), Modifier.padding(horizontal = 16.dp))
            SurfaceRow(
                title = stringResource(R.string.trusted_wifi_names),
                expandedContent = {
                    TrustedNetworkTextBox(
                        autoTunnelState.settings.trustedNetworkSSIDs,
                        onDelete = { viewModel.removeTrustedNetworkName(it) },
                        currentText = currentText,
                        onSave = { ssid -> viewModel.saveTrustedNetworkName(ssid) },
                        onValueChange = { currentText = it },
                        supporting = {
                            if (autoTunnelState.settings.isWildcardsEnabled) WildcardsLabel()
                        },
                        modifier = Modifier.padding(top = 4.dp),
                    )
                },
            )
        }
        Column {
            GroupLabel(stringResource(R.string.tunnels), Modifier.padding(horizontal = 16.dp))
            SurfaceRow(
                leading = { Icon(Icons.Outlined.Map, contentDescription = null) },
                title = stringResource(R.string.tunnel_mapping),
                onClick = { navController.push(Route.PreferredTunnel(TunnelNetwork.WIFI)) },
            )
        }
    }
}
