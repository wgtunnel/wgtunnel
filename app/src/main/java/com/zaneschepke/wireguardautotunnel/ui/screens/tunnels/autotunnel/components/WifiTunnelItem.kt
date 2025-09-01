package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.autotunnel.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.components.TrustedNetworkTextBox
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.components.WildcardsLabel
import com.zaneschepke.wireguardautotunnel.ui.theme.iconSize

@Composable
fun WifiTunnelItem(
    tunnelNetworks: Set<String>,
    isWildcardsEnabled: Boolean,
    onSaveTunnelNetwork: (String) -> Unit,
    onDeleteTunnelNetwork: (String) -> Unit,
): SelectionItem {

    var currentText by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(tunnelNetworks) { currentText = "" }

    return SelectionItem(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Security,
                    contentDescription = stringResource(R.string.use_tunnel_on_wifi_name),
                    modifier = Modifier.size(iconSize),
                )
                Text(
                    text = stringResource(R.string.use_tunnel_on_wifi_name),
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            MaterialTheme.colorScheme.onSurface
                        ),
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
        },
        description = {
            TrustedNetworkTextBox(
                trustedNetworks = tunnelNetworks,
                onDelete = onDeleteTunnelNetwork,
                currentText = currentText,
                onSave = onSaveTunnelNetwork,
                onValueChange = { currentText = it },
                supporting = {
                    if (isWildcardsEnabled) {
                        WildcardsLabel()
                    }
                },
            )
        },
    )
}
