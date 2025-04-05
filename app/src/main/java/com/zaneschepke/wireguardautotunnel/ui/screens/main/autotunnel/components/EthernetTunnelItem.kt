package com.zaneschepke.wireguardautotunnel.ui.screens.main.autotunnel.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun EthernetTunnelItem(tunnelConf: TunnelConf, viewModel: AppViewModel): SelectionItem {
	return SelectionItem(
		leadingIcon = Icons.Outlined.SettingsEthernet,
		title = {
			Text(
				text = stringResource(R.string.ethernet_tunnel),
				style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
			)
		},
		description = {
			Text(
				text = stringResource(R.string.set_ethernet_tunnel),
				style = MaterialTheme.typography.bodySmall.copy(MaterialTheme.colorScheme.outline),
			)
		},
		trailing = {
			ScaledSwitch(
				checked = tunnelConf.isEthernetTunnel,
				onClick = { viewModel.handleEvent(AppEvent.ToggleEthernetTunnel(tunnelConf)) },
			)
		},
		onClick = { viewModel.handleEvent(AppEvent.ToggleEthernetTunnel(tunnelConf)) },
	)
}
