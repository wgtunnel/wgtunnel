package com.zaneschepke.wireguardautotunnel.ui.screens.main.tunneloptions.components

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.textbox.SubmitConfigurationTextBox
import com.zaneschepke.wireguardautotunnel.util.extensions.isValidIpv4orIpv6Address
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun pingConfigItem(tunnelConf: TunnelConf, viewModel: AppViewModel): SelectionItem {
    return SelectionItem(
        title = {},
        description = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SubmitConfigurationTextBox(
                    value = tunnelConf.pingTarget,
                    label = stringResource(R.string.set_custom_ping_target),
                    hint = stringResource(R.string.ip_or_hostname),
                    isErrorValue = {
                        it?.isNotBlank() == true &&
                            !it.isValidIpv4orIpv6Address() &&
                            !Patterns.DOMAIN_NAME.matcher(it).matches()
                    },
                    supportingText = { Text(stringResource(R.string.ping_target_description)) },
                    onSubmit = { ip ->
                        viewModel.handleEvent(AppEvent.SetTunnelPingTarget(tunnelConf, ip))
                    },
                )
            }
        },
    )
}
