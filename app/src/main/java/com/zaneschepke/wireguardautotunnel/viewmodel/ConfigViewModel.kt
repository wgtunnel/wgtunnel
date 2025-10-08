package com.zaneschepke.wireguardautotunnel.viewmodel

import androidx.lifecycle.ViewModel
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.GlobalEffectRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.domain.sideeffect.GlobalSideEffect
import com.zaneschepke.wireguardautotunnel.ui.state.ConfigProxy
import com.zaneschepke.wireguardautotunnel.ui.state.ConfigUiState
import com.zaneschepke.wireguardautotunnel.util.StringValue
import com.zaneschepke.wireguardautotunnel.util.extensions.asStringValue
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import org.amnezia.awg.config.BadConfigException
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import timber.log.Timber

@HiltViewModel(assistedFactory = ConfigViewModel.Factory::class)
class ConfigViewModel
@AssistedInject
constructor(
    private val tunnelRepository: TunnelRepository,
    private val globalEffectRepository: GlobalEffectRepository,
    @Assisted val tunnelId: Int?,
) : ContainerHost<ConfigUiState, Nothing>, ViewModel() {

    override val container =
        container<ConfigUiState, Nothing>(
            ConfigUiState(),
            buildSettings = { repeatOnSubscribedStopTimeout = 5000L },
        ) {
            tunnelRepository.flow.collect { tuns ->
                reduce {
                    val tunnel = tuns.firstOrNull { it.id == tunnelId }
                    ConfigUiState(
                        tuns.filter { it.id != tunnelId }.map { it.tunName }.toSet(),
                        false,
                        tunnel,
                    )
                }
            }
        }

    fun saveConfigProxy(configProxy: ConfigProxy, tunnelName: String) = intent {
        if (state.unavailableNames.contains(tunnelName))
            return@intent postSideEffect(
                GlobalSideEffect.Toast(StringValue.StringResource(R.string.tunnel_name_taken))
            )
        runCatching {
                val (wg, am) = configProxy.buildConfigs()
                val tunnelConf =
                    if (tunnelId == null) {
                        TunnelConf.tunnelConfFromQuick(am.toAwgQuickString(true, false), tunnelName)
                    } else {
                        state.tunnel?.copy(
                            tunName = tunnelName,
                            amQuick = am.toAwgQuickString(true, false),
                            wgQuick = wg.toWgQuickString(true),
                        )
                    }
                if (tunnelConf != null) {
                    tunnelRepository.save(tunnelConf)
                    postSideEffect(
                        GlobalSideEffect.Toast(
                            StringValue.StringResource(R.string.config_changes_saved)
                        )
                    )
                    postSideEffect(GlobalSideEffect.PopBackStack)
                }
            }
            .onFailure {
                Timber.e(it)
                val message =
                    when (it) {
                        is BadConfigException -> it.asStringValue()
                        is com.wireguard.config.BadConfigException -> it.asStringValue()
                        else -> StringValue.StringResource(R.string.unknown_error)
                    }
                postSideEffect(GlobalSideEffect.Snackbar(message))
            }
    }

    suspend fun postSideEffect(globalSideEffect: GlobalSideEffect) {
        globalEffectRepository.post(globalSideEffect)
    }

    @AssistedFactory
    interface Factory {
        fun create(tunnelId: Int?): ConfigViewModel
    }
}
