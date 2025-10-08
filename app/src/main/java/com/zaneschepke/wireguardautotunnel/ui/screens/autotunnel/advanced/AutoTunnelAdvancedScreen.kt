package com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.advanced

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.dropdown.LabelledDropdown
import com.zaneschepke.wireguardautotunnel.viewmodel.AutoTunnelViewModel

@Composable
fun AutoTunnelAdvancedScreen(viewModel: AutoTunnelViewModel = hiltViewModel()) {
    val autoTunnelState by viewModel.container.stateFlow.collectAsStateWithLifecycle()
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
        modifier =
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
    ) {
        LabelledDropdown(
            title = {
                Text(
                    stringResource(R.string.debounce_delay),
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                )
            },
            leading = { Icon(Icons.Outlined.PauseCircle, null) },
            onSelected = { selected -> viewModel.setDebounceDelay(selected!!) },
            options = (0..10).toList(),
            currentValue = autoTunnelState.settings.debounceDelaySeconds,
            optionToString = { it?.toString() ?: stringResource(R.string._default) },
        )
    }
}
