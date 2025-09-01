package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.splittunnel.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.domain.model.InstalledPackage
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.splittunnel.state.SplitOption

@Composable
fun SplitTunnelContent(
    splitConfig: Pair<SplitOption, Set<String>>,
    installedPackages: List<InstalledPackage>,
    onSplitOptionChange: (SplitOption) -> Unit,
    onAppSelectionToggle: (String, Boolean) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
    ) {
        SplitOptionSelector(
            selectedOption = splitConfig.first,
            onOptionChange = onSplitOptionChange,
        )
        if (splitConfig.first != SplitOption.ALL) {
            AppListSection(
                installedPackages = installedPackages,
                onAppSelectionToggle = onAppSelectionToggle,
                splitConfig = splitConfig,
            )
        }
    }
}
