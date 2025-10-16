package com.zaneschepke.wireguardautotunnel.ui.screens.settings.dns

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.model.DnsProtocol
import com.zaneschepke.wireguardautotunnel.data.model.DnsProvider
import com.zaneschepke.wireguardautotunnel.ui.common.dropdown.LabelledDropdown
import com.zaneschepke.wireguardautotunnel.viewmodel.DnsViewModel

@Composable
fun DnsSettingsScreen(viewModel: DnsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val dnsUiState by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    if (dnsUiState.isLoading) return

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        Column {
            LabelledDropdown(
                title = stringResource(R.string.dns_protocol),
                leading = { Icon(Icons.Outlined.Dns, contentDescription = null) },
                currentValue = dnsUiState.dnsSettings.dnsProtocol,
                onSelected = { selected -> selected?.let { viewModel.setDnsProtocol(it) } },
                options = DnsProtocol.entries,
                optionToString = { (it ?: DnsProtocol.SYSTEM).asString(context) },
            )
            AnimatedVisibility(dnsUiState.dnsSettings.dnsProtocol != DnsProtocol.SYSTEM) {
                LabelledDropdown(
                    title = stringResource(R.string.dns_provider),
                    leading = { Icon(Icons.Outlined.Cloud, contentDescription = null) },
                    currentValue =
                        dnsUiState.dnsSettings.dnsEndpoint?.let { DnsProvider.fromAddress(it) }
                            ?: DnsProvider.CLOUDFLARE,
                    onSelected = { selected -> selected?.let { viewModel.setDnsProvider(it) } },
                    options = DnsProvider.entries,
                    optionToString = { it?.name ?: DnsProvider.CLOUDFLARE.name },
                )
            }
        }
    }
}
