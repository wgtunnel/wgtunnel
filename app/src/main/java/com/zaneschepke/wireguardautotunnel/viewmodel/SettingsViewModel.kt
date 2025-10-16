package com.zaneschepke.wireguardautotunnel.viewmodel

import androidx.lifecycle.ViewModel
import com.zaneschepke.wireguardautotunnel.core.shortcut.ShortcutManager
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConfig
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
    private val tunnelsRepository: TunnelRepository,
    private val globalEffectRepository: GlobalEffectRepository,
) : ContainerHost<SettingUiState, Nothing>, ViewModel() {

    override val container =
        container<SettingUiState, Nothing>(
            SettingUiState(),
            buildSettings = { repeatOnSubscribedStopTimeout = 5000L },
        ) {
            intent {
                combine(
                        settingsRepository.flow,
                        tunnelsRepository.globalTunnelFlow,
                        tunnelsRepository.userTunnelsFlow,
                    ) { settings, tunnel, tunnels ->
                        state.copy(
                            settings = settings,
                            remoteKey = settings.remoteKey,
                            isRemoteEnabled = settings.isRemoteControlEnabled,
                            isPinLockEnabled = settings.isPinLockEnabled,
                            isLoading = false,
                            globalTunnelConfig = tunnel,
                            tunnels = tunnels,
                        )
                    }
                    .collect { reduce { it } }
            }
        }

    fun setShortcutsEnabled(to: Boolean) = intent {
        if (to) shortcutManager.addShortcuts() else shortcutManager.removeShortcuts()
        settingsRepository.upsert(state.settings.copy(isShortcutsEnabled = to))
    }

    suspend fun postSideEffect(globalSideEffect: GlobalSideEffect) {
        globalEffectRepository.post(globalSideEffect)
    }

    fun setAlwaysOnVpnEnabled(to: Boolean) = intent {
        settingsRepository.upsert(state.settings.copy(isAlwaysOnVpnEnabled = to))
    }

    fun setRestoreOnBootEnabled(to: Boolean) = intent {
        settingsRepository.upsert(state.settings.copy(isRestoreOnBootEnabled = to))
    }

    fun setLanKillSwitchEnabled(to: Boolean) = intent {
        settingsRepository.upsert(state.settings.copy(isLanOnKillSwitchEnabled = to))
    }

    fun setTunnelGlobals(to: Boolean) = intent {
        settingsRepository.upsert(state.settings.copy(isTunnelGlobalsEnabled = to))
        if (state.globalTunnelConfig == null)
            tunnelsRepository.save(TunnelConfig.generateDefaultGlobalConfig())
    }

    fun setRemoteEnabled(to: Boolean) = intent {
        settingsRepository.upsert(
            state.settings.copy(
                isRemoteControlEnabled = to,
                remoteKey = UUID.randomUUID().toString(),
            )
        )
    }
}
