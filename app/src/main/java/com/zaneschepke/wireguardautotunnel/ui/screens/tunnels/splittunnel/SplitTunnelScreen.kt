package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.splittunnel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.LocalSharedVm
import com.zaneschepke.wireguardautotunnel.ui.common.button.ActionIconButton
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.splittunnel.components.SplitTunnelContent
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.splittunnel.state.SplitOption
import com.zaneschepke.wireguardautotunnel.ui.state.NavbarState
import com.zaneschepke.wireguardautotunnel.viewmodel.SplitTunnelViewModel

@Composable
fun SplitTunnelScreen(tunnelId: Int, viewModel: SplitTunnelViewModel = hiltViewModel()) {
    val splitTunnelState by viewModel.container.stateFlow.collectAsStateWithLifecycle()
    val sharedViewModel = LocalSharedVm.current

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

    LaunchedEffect(Unit) {
        sharedViewModel.updateNavbarState(
            NavbarState(
                topTitle = { Text(tunnelConf.name) },
                topTrailing = {
                    ActionIconButton(Icons.Rounded.Save, R.string.save) {
                        viewModel.saveSplitTunnelSelection(tunnelId, splitConfig)
                    }
                },
                showTopItems = true,
                showBottomItems = true,
            )
        )
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
