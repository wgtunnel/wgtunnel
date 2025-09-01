package com.zaneschepke.wireguardautotunnel.viewmodel

import androidx.lifecycle.ViewModel
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.repository.GlobalEffectRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.InstalledPackageRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.domain.sideeffect.GlobalSideEffect
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.splittunnel.state.SplitOption
import com.zaneschepke.wireguardautotunnel.ui.state.ConfigProxy
import com.zaneschepke.wireguardautotunnel.ui.state.InterfaceProxy
import com.zaneschepke.wireguardautotunnel.ui.state.SplitTunnelUiState
import com.zaneschepke.wireguardautotunnel.util.StringValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class SplitTunnelViewModel
@Inject
constructor(
    private val tunnelRepository: TunnelRepository,
    private val packageRepository: InstalledPackageRepository,
    private val globalEffectRepository: GlobalEffectRepository,
) : ContainerHost<SplitTunnelUiState, Nothing>, ViewModel() {

    override val container =
        container<SplitTunnelUiState, Nothing>(
            SplitTunnelUiState(),
            buildSettings = { repeatOnSubscribedStopTimeout = 5000L },
        ) {
            val packagesFlow = flow {
                val packages = packageRepository.getInstalledPackages()
                emit(packages)
            }

            combine(packagesFlow, tunnelRepository.flow) { packages, tunnels ->
                    SplitTunnelUiState(packages, true, tunnels)
                }
                .collect { reduce { it } }
        }

    suspend fun postSideEffect(globalSideEffect: GlobalSideEffect) {
        globalEffectRepository.post(globalSideEffect)
    }

    fun saveSplitTunnelSelection(tunnelId: Int, splitConfig: Pair<SplitOption, Set<String>>) =
        intent {
            val latestTunnel = state.tunnels.find { it.id == tunnelId }
            if (latestTunnel != null) {
                val config = latestTunnel.toAmConfig()
                val (option, pkgs) = splitConfig
                val configProxy = ConfigProxy.from(config)
                val interfaceProxy = InterfaceProxy.from(config.`interface`)
                val (included, excluded) =
                    when (option) {
                        SplitOption.INCLUDE -> Pair(pkgs, emptySet<String>())
                        SplitOption.ALL -> Pair(emptySet(), emptySet())
                        SplitOption.EXCLUDE -> Pair(emptySet(), pkgs)
                    }
                val updatedInterface =
                    interfaceProxy.copy(
                        includedApplications = included,
                        excludedApplications = excluded,
                    )
                val updatedConfig = configProxy.copy(`interface` = updatedInterface)
                val (wg, am) = updatedConfig.buildConfigs()
                tunnelRepository.save(
                    latestTunnel.copy(
                        amQuick = am.toAwgQuickString(true, false),
                        wgQuick = wg.toWgQuickString(true),
                    )
                )
                postSideEffect(
                    GlobalSideEffect.Snackbar(
                        StringValue.StringResource(R.string.config_changes_saved)
                    )
                )
                postSideEffect(GlobalSideEffect.PopBackStack)
            }
        }
}
