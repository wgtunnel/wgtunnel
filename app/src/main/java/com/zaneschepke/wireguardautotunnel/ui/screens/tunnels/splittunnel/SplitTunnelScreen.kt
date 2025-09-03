package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.splittunnel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.ui.LocalSharedVm
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.splittunnel.components.SplitTunnelContent
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.splittunnel.state.SplitOption
import com.zaneschepke.wireguardautotunnel.ui.sideeffect.LocalSideEffect
import com.zaneschepke.wireguardautotunnel.viewmodel.SplitTunnelViewModel
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun SplitTunnelScreen(tunnelId: Int, viewModel: SplitTunnelViewModel = hiltViewModel()) {
    val sharedViewModel = LocalSharedVm.current
    val splitTunnelState by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    if (!splitTunnelState.stateInitialized) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(30.dp), strokeWidth = 5.dp)
        }
        return
    }
    val tunnelConf by
        remember(splitTunnelState.tunnels) {
            derivedStateOf { splitTunnelState.tunnels.find { it.id == tunnelId }!! }
        }

    val conf by remember { derivedStateOf { tunnelConf.toAmConfig() } }

    var splitConfig by remember {
        mutableStateOf<Pair<SplitOption, Set<String>>>(
            when {
                conf.`interface`.excludedApplications.isNotEmpty() ->
                    Pair(SplitOption.EXCLUDE, conf.`interface`.excludedApplications.toSet())
                conf.`interface`.includedApplications.isNotEmpty() ->
                    Pair(SplitOption.INCLUDE, conf.`interface`.includedApplications.toSet())
                else -> Pair(SplitOption.ALL, emptySet())
            }
        )
    }

    sharedViewModel.collectSideEffect { sideEffect ->
        if (sideEffect is LocalSideEffect.SaveChanges)
            viewModel.saveSplitTunnelSelection(tunnelId, splitConfig)
    }

    SplitTunnelContent(
        splitConfig = splitConfig,
        installedPackages = splitTunnelState.installedPackages,
        onSplitOptionChange = { splitConfig = Pair(it, splitConfig.second) },
        onAppSelectionToggle = { appPackage, enabled ->
            val updated =
                splitConfig.second.toMutableSet().apply {
                    if (!enabled) remove(appPackage) else add(appPackage)
                }
            splitConfig = Pair(splitConfig.first, updated)
        },
    )
}
