package com.zaneschepke.wireguardautotunnel.ui.screens.settings.dns

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.model.DnsProtocol
import com.zaneschepke.wireguardautotunnel.data.model.DnsProvider
import com.zaneschepke.wireguardautotunnel.ui.common.dropdown.LabelledDropdown
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun DnsSettingsScreen(uiState: AppUiState, viewModel: AppViewModel) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
        modifier =
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp)
                .padding(horizontal = 12.dp),
    ) {
        LabelledDropdown(
            title = {
                Text(
                    text = stringResource(R.string.dns_protocol),
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                )
            },
            leading = { Icon(Icons.Outlined.Dns, contentDescription = null) },
            currentValue = uiState.appSettings.dnsProtocol,
            onSelected = { selected ->
                selected?.let { viewModel.handleEvent(AppEvent.SetDnsProtocol(it)) }
            },
            options = DnsProtocol.entries,
            optionToString = { (it ?: DnsProtocol.SYSTEM).asString(context) },
        )
        AnimatedVisibility(uiState.appSettings.dnsProtocol != DnsProtocol.SYSTEM) {
            LabelledDropdown(
                title = {
                    Text(
                        text = stringResource(R.string.dns_provider),
                        style =
                            MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                    )
                },
                leading = { Icon(Icons.Outlined.Cloud, contentDescription = null) },
                currentValue =
                    uiState.appSettings.dnsEndpoint?.let { DnsProvider.fromAddress(it) }
                        ?: DnsProvider.CLOUDFLARE,
                onSelected = { selected ->
                    selected?.let { viewModel.handleEvent(AppEvent.SetDnsProvider(it)) }
                },
                options = DnsProvider.entries,
                optionToString = { it?.name ?: DnsProvider.CLOUDFLARE.name },
            )
        }
    }
}
