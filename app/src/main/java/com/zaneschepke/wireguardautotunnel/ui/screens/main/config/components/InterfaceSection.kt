package com.zaneschepke.wireguardautotunnel.ui.screens.main.config.components

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.config.ConfigurationTextBox
import com.zaneschepke.wireguardautotunnel.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.ui.screens.main.config.ConfigViewModel
import com.zaneschepke.wireguardautotunnel.ui.screens.main.config.state.ConfigUiState
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledWidth

@Composable
fun InterfaceSection(uiState: ConfigUiState, viewModel: ConfigViewModel) {
	var isDropDownExpanded by remember { mutableStateOf(false) }

	Surface(
		shape = RoundedCornerShape(12.dp),
		color = MaterialTheme.colorScheme.surface,
	) {
		Column(
			verticalArrangement = Arrangement.spacedBy(5.dp),
			modifier = Modifier.padding(16.dp.scaledWidth()).focusGroup(),
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
					isAmneziaCompatibilitySet = uiState.configProxy.`interface`.isAmneziaCompatibilityModeSet(),
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
