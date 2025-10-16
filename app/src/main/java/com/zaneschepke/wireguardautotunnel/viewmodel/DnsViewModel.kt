package com.zaneschepke.wireguardautotunnel.viewmodel

import androidx.lifecycle.ViewModel
import com.zaneschepke.wireguardautotunnel.data.model.DnsProtocol
import com.zaneschepke.wireguardautotunnel.data.model.DnsProvider
import com.zaneschepke.wireguardautotunnel.domain.repository.DnsSettingsRepository
import com.zaneschepke.wireguardautotunnel.ui.state.DnsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

@HiltViewModel
class DnsViewModel @Inject constructor(private val dnsSettingsRepository: DnsSettingsRepository) :
    ContainerHost<DnsUiState, Nothing>, ViewModel() {

    override val container =
        container<DnsUiState, Nothing>(
            DnsUiState(),
            buildSettings = { repeatOnSubscribedStopTimeout = 5000L },
        ) {
            dnsSettingsRepository.flow.collect {
                reduce { state.copy(dnsSettings = it, isLoading = false) }
            }
        }

    fun setDnsProtocol(to: DnsProtocol) = intent {
        dnsSettingsRepository.upsert(state.dnsSettings.copy(dnsProtocol = to))
    }

    fun setDnsProvider(dnsProvider: DnsProvider) = intent {
        dnsSettingsRepository.upsert(
            state.dnsSettings.copy(
                dnsEndpoint = dnsProvider.asAddress(state.dnsSettings.dnsProtocol)
            )
        )
    }
}
