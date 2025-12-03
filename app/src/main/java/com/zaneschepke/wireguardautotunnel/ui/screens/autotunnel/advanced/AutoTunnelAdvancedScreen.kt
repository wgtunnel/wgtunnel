package com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.advanced

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.ui.common.button.ThemedSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.dropdown.LabelledDropdown
import com.zaneschepke.wireguardautotunnel.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.ui.common.text.DescriptionText
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.components.TrustedNetworkTextBox
import com.zaneschepke.wireguardautotunnel.viewmodel.AutoTunnelViewModel

@Composable
fun AutoTunnelAdvancedScreen(viewModel: AutoTunnelViewModel = hiltViewModel()) {
    val autoTunnelState by viewModel.container.stateFlow.collectAsStateWithLifecycle()
    
    var currentRoamingText by remember { mutableStateOf("") }

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        Column {
            GroupLabel(
                stringResource(R.string.reliability),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            LabelledDropdown(
                title = stringResource(R.string.debounce_delay),
                description = { DescriptionText(stringResource(R.string.debounce_description)) },
                leading = { Icon(Icons.Outlined.PauseCircle, null) },
                onSelected = { selected -> viewModel.setDebounceDelay(selected!!) },
                options = (0..10).toList(),
                currentValue = autoTunnelState.autoTunnelSettings.debounceDelaySeconds,
                optionToString = { it?.toString() ?: stringResource(R.string._default) },
            )
        }

        // --- ROAMING SECTION ---
        Column {
            GroupLabel(
                "Roaming (BSSID)",
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp),
            )
            
            // 1. Master Toggle
            SurfaceRow(
                leading = { Icon(Icons.Outlined.SettingsEthernet, contentDescription = null) },
                title = "Restart on BSSID change",
                description = { DescriptionText("Restart tunnel when switching access points on the same SSID") },
                trailing = {
                    ThemedSwitch(
                        checked = autoTunnelState.autoTunnelSettings.isBssidRoamingEnabled,
                        onClick = { viewModel.setBssidRoamingEnabled(it) },
                    )
                },
                onClick = { viewModel.setBssidRoamingEnabled(!autoTunnelState.autoTunnelSettings.isBssidRoamingEnabled) },
            )

            if (autoTunnelState.autoTunnelSettings.isBssidRoamingEnabled) {
                
                // 2. Auto-Save Toggle
                SurfaceRow(
                    title = "Auto-detect networks",
                    description = { DescriptionText("Automatically add the current network to the list below if roaming is detected.") },
                    trailing = {
                        ThemedSwitch(
                            checked = autoTunnelState.autoTunnelSettings.isBssidAutoSaveEnabled,
                            onClick = { viewModel.setBssidAutoSaveEnabled(it) },
                        )
                    },
                    onClick = { viewModel.setBssidAutoSaveEnabled(!autoTunnelState.autoTunnelSettings.isBssidAutoSaveEnabled) },
                )

                // 3. Restrict to List Toggle
                SurfaceRow(
                    title = "Restrict to saved networks",
                    description = { DescriptionText("If disabled, roaming restart will happen on ALL networks (Global Mode).") },
                    trailing = {
                        ThemedSwitch(
                            checked = autoTunnelState.autoTunnelSettings.isBssidListEnabled,
                            onClick = { viewModel.setBssidListEnabled(it) },
                        )
                    },
                    onClick = { viewModel.setBssidListEnabled(!autoTunnelState.autoTunnelSettings.isBssidListEnabled) },
                )

                // 4. The List
                if (autoTunnelState.autoTunnelSettings.isBssidListEnabled) {
                    SurfaceRow(
                        title = "Roaming SSIDs",
                        description = { DescriptionText("List of networks where roaming restart is allowed.") },
                        expandedContent = {
                            TrustedNetworkTextBox(
                                trustedNetworks = autoTunnelState.autoTunnelSettings.roamingSSIDs,
                                onDelete = { viewModel.removeRoamingSSID(it) },
                                currentText = currentRoamingText,
                                onSave = { 
                                    viewModel.saveRoamingSSID(it) 
                                    currentRoamingText = "" 
                                },
                                onValueChange = { currentRoamingText = it },
                                supporting = { DescriptionText("Add specific SSIDs (e.g. 'MyHomeWiFi')") },
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        },
                    )
                }
            }
        }
    }
}
