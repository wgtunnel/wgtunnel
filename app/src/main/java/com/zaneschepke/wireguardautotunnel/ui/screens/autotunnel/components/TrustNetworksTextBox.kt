package com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ClickableIconButton
import com.zaneschepke.wireguardautotunnel.ui.common.textbox.CustomTextField
import com.zaneschepke.wireguardautotunnel.util.extensions.isRunningOnTv

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrustedNetworkTextBox(
    trustedNetworks: List<String>,
    onDelete: (ssid: String) -> Unit,
    currentText: String,
    onSave: (ssid: String) -> Unit,
    onValueChange: (network: String) -> Unit,
    supporting: @Composable () -> Unit,
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        ) {
            trustedNetworks.forEach { ssid ->
                ClickableIconButton(
                    onClick = {
                        if (context.isRunningOnTv()) {
                            onDelete(ssid)
                        }
                    },
                    onIconClick = { onDelete(ssid) },
                    text = ssid,
                    icon = Icons.Filled.Close,
                )
            }
        }
        CustomTextField(
            textStyle =
                MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
            value = currentText,
            onValueChange = onValueChange,
            interactionSource = remember { MutableInteractionSource() },
            label = {
                Text(
                    stringResource(R.string.add_wifi_name),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelMedium,
                )
            },
            containerColor = MaterialTheme.colorScheme.surface,
            supportingText = supporting,
            modifier =
                Modifier.padding(top = 5.dp, bottom = 10.dp).fillMaxWidth().padding(end = 16.dp),
            singleLine = true,
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Done,
                ),
            keyboardActions = KeyboardActions(onDone = { onSave(currentText) }),
            trailing = {
                if (currentText != "") {
                    IconButton(onClick = { onSave(currentText) }) {
                        val icon = Icons.Outlined.Add
                        Icon(
                            imageVector = icon,
                            contentDescription =
                                stringResource(R.string.trusted_ssid_value_description),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            },
        )
    }
}
