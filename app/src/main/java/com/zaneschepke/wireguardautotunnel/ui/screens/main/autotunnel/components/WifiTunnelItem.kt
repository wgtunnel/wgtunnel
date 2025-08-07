package com.zaneschepke.wireguardautotunnel.ui.screens.main.autotunnel.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.model.AppSettings
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.components.TrustedNetworkTextBox
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.components.WildcardsLabel
import com.zaneschepke.wireguardautotunnel.ui.theme.iconSize
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun WifiTunnelItem(
    tunnelConf: TunnelConf,
    appSettings: AppSettings,
    viewModel: AppViewModel,
    currentText: String,
    onTextChange: (String) -> Unit,
): SelectionItem {
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
                trustedNetworks = tunnelConf.tunnelNetworks,
                onDelete = { viewModel.handleEvent(AppEvent.DeleteTunnelRunSSID(it, tunnelConf)) },
                currentText = currentText,
                onSave = {
                    viewModel.handleEvent(AppEvent.AddTunnelRunSSID(it, tunnelConf))
                    onTextChange("") // Reset the text field after saving
                },
                onValueChange = onTextChange,
                supporting = {
                    if (appSettings.isWildcardsEnabled) {
                        WildcardsLabel()
                    }
                },
            )
        },
    )
}
