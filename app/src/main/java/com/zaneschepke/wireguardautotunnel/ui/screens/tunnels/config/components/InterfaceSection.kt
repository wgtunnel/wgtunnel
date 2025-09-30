package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.config.components

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.ui.common.textbox.ConfigurationTextBox
import com.zaneschepke.wireguardautotunnel.ui.state.ConfigProxy
import com.zaneschepke.wireguardautotunnel.ui.state.InterfaceProxy
import java.util.*

@Composable
fun InterfaceSection(
    isGlobalConfig: Boolean,
    configProxy: ConfigProxy,
    tunnelName: String,
    isTunnelNameTaken: Boolean,
    onInterfaceChange: (InterfaceProxy) -> Unit,
    onTunnelNameChange: (String) -> Unit,
    onMimicQuic: () -> Unit,
    onMimicDns: () -> Unit,
    onMimicSip: () -> Unit,
) {
    var showAmneziaValues by rememberSaveable {
        mutableStateOf(configProxy.`interface`.isAmneziaEnabled())
    }

    var showScripts by rememberSaveable { mutableStateOf(configProxy.hasScripts()) }
    var isDropDownExpanded by rememberSaveable { mutableStateOf(false) }
    val isAmneziaCompatibilitySet =
        remember(configProxy.`interface`) {
            configProxy.`interface`.isAmneziaCompatibilityModeSet()
        }

    fun toggleAmneziaCompat() {
        val (show, interfaceProxy) =
            if (configProxy.`interface`.isAmneziaCompatibilityModeSet()) {
                Pair(false, configProxy.`interface`.resetAmneziaProperties())
            } else Pair(true, configProxy.`interface`.toAmneziaCompatibilityConfig())
        showAmneziaValues = show
        onInterfaceChange(interfaceProxy)
    }

    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 16.dp).focusGroup(),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                GroupLabel(stringResource(R.string.interface_))
                InterfaceDropdown(
                    expanded = isDropDownExpanded,
                    onExpandedChange = { isDropDownExpanded = it },
                    showScripts = showScripts,
                    showAmneziaValues = showAmneziaValues,
                    isAmneziaCompatibilitySet = isAmneziaCompatibilitySet,
                    onToggleScripts = { showScripts = !showScripts },
                    onToggleAmneziaValues = { showAmneziaValues = !showAmneziaValues },
                    onToggleAmneziaCompatibility = { toggleAmneziaCompat() },
                    onMimicQuic = {
                        showAmneziaValues = true
                        onMimicQuic()
                    },
                    onMimicDns = {
                        showAmneziaValues = true
                        onMimicDns()
                    },
                    onMimicSip = {
                        showAmneziaValues = true
                        onMimicSip()
                    },
                )
            }
            if (!isGlobalConfig)
                ConfigurationTextBox(
                    value = tunnelName,
                    onValueChange = onTunnelNameChange,
                    label = stringResource(R.string.name),
                    isError = isTunnelNameTaken,
                    hint =
                        stringResource(R.string.hint_template, stringResource(R.string.tunnel_name))
                            .lowercase(Locale.getDefault()),
                    modifier = Modifier.fillMaxWidth(),
                )
            InterfaceFields(
                isGlobalConfig,
                interfaceState = configProxy.`interface`,
                showScripts = showScripts,
                showAmneziaValues = showAmneziaValues,
                onInterfaceChange = onInterfaceChange,
            )
        }
    }
}
