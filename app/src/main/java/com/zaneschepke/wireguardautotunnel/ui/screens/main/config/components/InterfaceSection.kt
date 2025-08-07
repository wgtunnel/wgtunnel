package com.zaneschepke.wireguardautotunnel.ui.screens.main.config.components

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.config.ConfigurationTextBox
import com.zaneschepke.wireguardautotunnel.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.ui.screens.main.config.ConfigViewModel
import com.zaneschepke.wireguardautotunnel.ui.screens.main.config.state.ConfigUiState

@Composable
fun InterfaceSection(uiState: ConfigUiState, viewModel: ConfigViewModel) {
    var isDropDownExpanded by remember { mutableStateOf(false) }
    val isAmneziaCompatibilitySet =
        remember(uiState.configProxy.`interface`) {
            uiState.configProxy.`interface`.isAmneziaCompatibilityModeSet()
        }

    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(16.dp).focusGroup(),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                GroupLabel(stringResource(R.string.interface_))
                InterfaceDropdown(
                    expanded = isDropDownExpanded,
                    onExpandedChange = { isDropDownExpanded = it },
                    showScripts = uiState.showScripts,
                    showAmneziaValues = uiState.showAmneziaValues,
                    isAmneziaCompatibilitySet = isAmneziaCompatibilitySet,
                    onToggleScripts = viewModel::toggleScripts,
                    onToggleAmneziaValues = viewModel::toggleAmneziaValues,
                    onToggleAmneziaCompatibility = viewModel::toggleAmneziaCompatibility,
                )
            }
            ConfigurationTextBox(
                value = uiState.tunnelName,
                onValueChange = viewModel::updateTunnelName,
                label = stringResource(R.string.name),
                hint = stringResource(R.string.tunnel_name).lowercase(),
                modifier = Modifier.fillMaxWidth(),
            )
            InterfaceFields(
                interfaceState = uiState.configProxy.`interface`,
                showAuthPrompt = { viewModel.toggleShowAuthPrompt() },
                showScripts = uiState.showScripts,
                showAmneziaValues = uiState.showAmneziaValues,
                onInterfaceChange = viewModel::updateInterface,
                isAuthenticated = uiState.isAuthenticated,
            )
        }
    }
}
