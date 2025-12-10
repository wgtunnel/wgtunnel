package com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.advanced

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Filter1
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
import androidx.compose.ui.platform.LocalContext
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
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.components.WildcardsLabel
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.LearnMoreLinkLabel
import com.zaneschepke.wireguardautotunnel.util.extensions.openWebUrl
import com.zaneschepke.wireguardautotunnel.viewmodel.AutoTunnelViewModel

@Composable
fun AutoTunnelAdvancedScreen(viewModel: AutoTunnelViewModel = hiltViewModel()) {
    val context = LocalContext.current
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
                stringResource(R.string.roaming_bssid),
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp),
            )
            
            // 1. Master Toggle
            SurfaceRow(
                leading = { Icon(Icons.Outlined.SettingsEthernet, contentDescription = null) },
                title = stringResource(R.string.restart_on_bssid_change),
                description = { DescriptionText(stringResource(R.string.restart_on_bssid_change_description)) },
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
                    title = stringResource(R.string.auto_detect_networks),
                    description = { DescriptionText(stringResource(R.string.auto_detect_networks_description)) },
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
                    title = stringResource(R.string.restrict_to_saved_networks),
                    description = { DescriptionText(stringResource(R.string.restrict_to_saved_networks_description)) },
                    trailing = {
                        ThemedSwitch(
                            checked = autoTunnelState.autoTunnelSettings.isBssidListEnabled,
                            onClick = { viewModel.setBssidListEnabled(it) },
                        )
                    },
                    onClick = { viewModel.setBssidListEnabled(!autoTunnelState.autoTunnelSettings.isBssidListEnabled) },
                )

                // 4. Roaming Wildcards Toggle
                SurfaceRow(
                    leading = { Icon(Icons.Outlined.Filter1, contentDescription = null) },
                    title = stringResource(R.string.roaming_wildcards),
                    description = {
                        LearnMoreLinkLabel(
                            { context.openWebUrl(it) },
                            stringResource(R.string.docs_wildcards),
                        )
                    },
                    trailing = {
                        ThemedSwitch(
                            checked = autoTunnelState.autoTunnelSettings.isBssidWildcardsEnabled,
                            onClick = { viewModel.setBssidWildcardsEnabled(it) },
                        )
                    },
                    onClick = { viewModel.setBssidWildcardsEnabled(!autoTunnelState.autoTunnelSettings.isBssidWildcardsEnabled) },
                )

                // 5. The List
                if (autoTunnelState.autoTunnelSettings.isBssidListEnabled) {
                    SurfaceRow(
                        title = stringResource(R.string.roaming_ssids),
                        description = { DescriptionText(stringResource(R.string.roaming_ssids_description)) },
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
                                supporting = { 
                                    if (autoTunnelState.autoTunnelSettings.isBssidWildcardsEnabled) WildcardsLabel()
                                },
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        },
                    )
                }
            }
        }
    }
}
