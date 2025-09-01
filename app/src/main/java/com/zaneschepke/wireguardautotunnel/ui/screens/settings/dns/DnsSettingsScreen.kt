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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.model.DnsProtocol
import com.zaneschepke.wireguardautotunnel.data.model.DnsProvider
import com.zaneschepke.wireguardautotunnel.ui.LocalSharedVm
import com.zaneschepke.wireguardautotunnel.ui.common.dropdown.LabelledDropdown
import com.zaneschepke.wireguardautotunnel.ui.state.NavbarState
import com.zaneschepke.wireguardautotunnel.viewmodel.SettingsViewModel

@Composable
fun DnsSettingsScreen(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val sharedViewModel = LocalSharedVm.current
    val settingsState by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        sharedViewModel.updateNavbarState(
            NavbarState(
                showTopItems = true,
                showBottomItems = true,
                topTitle = { Text(stringResource(R.string.dns_settings)) },
            )
        )
    }

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
            currentValue = settingsState.settings.dnsProtocol,
            onSelected = { selected -> selected?.let { viewModel.setDnsProtocol(it) } },
            options = DnsProtocol.entries,
            optionToString = { (it ?: DnsProtocol.SYSTEM).asString(context) },
        )
        AnimatedVisibility(settingsState.settings.dnsProtocol != DnsProtocol.SYSTEM) {
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
                    settingsState.settings.dnsEndpoint?.let { DnsProvider.fromAddress(it) }
                        ?: DnsProvider.CLOUDFLARE,
                onSelected = { selected -> selected?.let { viewModel.setDnsProvider(it) } },
                options = DnsProvider.entries,
                optionToString = { it?.name ?: DnsProvider.CLOUDFLARE.name },
            )
        }
    }
}
