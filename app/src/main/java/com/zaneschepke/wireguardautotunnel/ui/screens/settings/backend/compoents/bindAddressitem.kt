package com.zaneschepke.wireguardautotunnel.ui.screens.settings.backend.compoents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.config.SubmitConfigurationTextBox
import com.zaneschepke.wireguardautotunnel.util.extensions.isValidProxyBindAddress

@Composable
fun bindAddressItem(value: String, label : String, hint : String, onSubmit: (ip : String) -> Unit) : SelectionItem {
    return SelectionItem(
        title = {},
        description = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SubmitConfigurationTextBox(
                    value = value,
                    label = label,
                    hint = hint,
                    isErrorValue = {
                        it?.let { it.isNotBlank() && !it.isValidProxyBindAddress() } ?: false
                    },
                    onSubmit = onSubmit,
                )
            }
        },
    )
}