package com.zaneschepke.wireguardautotunnel.ui.screens.main.config.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.screens.main.config.ConfigViewModel

@Composable
fun AddPeerButton(viewModel: ConfigViewModel) {
	Row(
		horizontalArrangement = Arrangement.Center,
		verticalAlignment = Alignment.CenterVertically,
		modifier = Modifier.fillMaxWidth().padding(bottom = 140.dp),
	) {
		TextButton(onClick = { viewModel.addPeer() }) {
			Text(stringResource(R.string.add_peer))
		}
	}
}
