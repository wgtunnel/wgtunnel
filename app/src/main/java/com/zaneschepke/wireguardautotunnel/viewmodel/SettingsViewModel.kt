package com.zaneschepke.wireguardautotunnel.viewmodel

import androidx.lifecycle.ViewModel
import com.zaneschepke.wireguardautotunnel.core.shortcut.ShortcutManager
import com.zaneschepke.wireguardautotunnel.data.model.DnsProtocol
import com.zaneschepke.wireguardautotunnel.data.model.DnsProvider
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.AppStateRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.GeneralSettingRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.GlobalEffectRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.domain.sideeffect.GlobalSideEffect
import com.zaneschepke.wireguardautotunnel.ui.state.SettingUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    private val settingsRepository: GeneralSettingRepository,
    private val shortcutManager: ShortcutManager,
    private val appStateRepository: AppStateRepository,
    private val tunnelsRepository: TunnelRepository,
    private val globalEffectRepository: GlobalEffectRepository,
) : ContainerHost<SettingUiState, Nothing>, ViewModel() {

    override val container =
        container<SettingUiState, Nothing>(
            SettingUiState(),
            buildSettings = { repeatOnSubscribedStopTimeout = 5000L },
        ) {
            intent {
                combine(settingsRepository.flow, appStateRepository.flow, tunnelsRepository.globalTunnelFlow) { settings, appState, tunnel ->
                        SettingUiState(
                            settings = settings,
                            isLocalLoggingEnabled = appState.isLocalLogsEnabled,
                            remoteKey = appState.remoteKey,
                            isRemoteEnabled = appState.isRemoteControlEnabled,
                            isPinLockEnabled = appState.isPinLockEnabled,
                            showDetailedPingStats = appState.showDetailedPingStats,
                            stateInitialized = true,
                            globalTunnelConf = tunnel
                        )
                    }
                    .collect { reduce { it } }
            }
        }

    fun setPingEnabled(to: Boolean) = intent {
        settingsRepository.save(state.settings.copy(isPingEnabled = to))
    }

    fun setShortcutsEnabled(to: Boolean) = intent {
        if (to) shortcutManager.addShortcuts() else shortcutManager.removeShortcuts()
        settingsRepository.save(state.settings.copy(isShortcutsEnabled = to))
    }

    suspend fun postSideEffect(globalSideEffect: GlobalSideEffect) {
        globalEffectRepository.post(globalSideEffect)
    }

    fun setAlwaysOnVpnEnabled(to: Boolean) = intent {
        settingsRepository.save(state.settings.copy(isAlwaysOnVpnEnabled = to))
    }

    fun setRestoreOnBootEnabled(to: Boolean) = intent {
        settingsRepository.save(state.settings.copy(isRestoreOnBootEnabled = to))
    }

    fun setLanKillSwitchEnabled(to: Boolean) = intent {
        settingsRepository.save(state.settings.copy(isLanOnKillSwitchEnabled = to))
    }

    fun setTunnelGlobals(to: Boolean) = intent {
        settingsRepository.save(state.settings.copy(isTunnelGlobalsEnabled = to))
        if(state.globalTunnelConf == null) tunnelsRepository.save(TunnelConf.generateDefaultGlobalConfig())
    }

    fun setTunnelPingIntervalSeconds(to: Int) = intent {
        settingsRepository.save(state.settings.copy(tunnelPingIntervalSeconds = to))
    }

    fun setTunnelPingAttempts(to: Int) = intent {
        settingsRepository.save(state.settings.copy(tunnelPingAttempts = to))
    }

    fun setTunnelPingTimeoutSeconds(to: Int?) = intent {
        settingsRepository.save(state.settings.copy(tunnelPingTimeoutSeconds = to))
    }

    fun setDnsProtocol(to: DnsProtocol) = intent {
        settingsRepository.save(state.settings.copy(dnsProtocol = to))
    }

    fun setDetailedPingStats(to: Boolean) = intent {
        appStateRepository.setShowDetailedPingStats(to)
    }

    fun setLocalLogging(to: Boolean) = intent { appStateRepository.setLocalLogsEnabled(to) }

    fun setRemoteEnabled(to: Boolean) = intent {
        appStateRepository.setRemoteKey(UUID.randomUUID().toString())
        appStateRepository.setIsRemoteControlEnabled(to)
    }

    fun setDnsProvider(dnsProvider: DnsProvider) = intent {
        settingsRepository.save(
            state.settings.copy(dnsEndpoint = dnsProvider.asAddress(state.settings.dnsProtocol))
        )
    }
}
