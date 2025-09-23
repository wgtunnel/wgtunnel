package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.tunneloptions.components

import android.util.Patterns
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.textbox.CustomTextField
import com.zaneschepke.wireguardautotunnel.util.extensions.isValidIpv4orIpv6Address

@Composable
fun pingConfigItem(tunnelConf: TunnelConf, onSubmit: (ip: String) -> Unit): SelectionItem {
    return SelectionItem(
        title = {},
        description = {
            val focusManager = LocalFocusManager.current
            val keyboardController = LocalSoftwareKeyboardController.current

            var stateValue by remember { mutableStateOf(tunnelConf.pingTarget ?: "") }
            val isError by
                remember(stateValue) {
                    derivedStateOf {
                        stateValue.isNotBlank() &&
                            !stateValue.isValidIpv4orIpv6Address() &&
                            !Patterns.DOMAIN_NAME.matcher(stateValue).matches()
                    }
                }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CustomTextField(
                    isError = isError,
                    textStyle =
                        MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                    value = stateValue,
                    onValueChange = { stateValue = it },
                    interactionSource = remember { MutableInteractionSource() },
                    label = {
                        Text(
                            stringResource(R.string.set_custom_ping_target),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    placeholder = {
                        Text(
                            stringResource(R.string.ip_or_hostname),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    supportingText = { Text(stringResource(R.string.ping_target_description)) },
                    modifier =
                        Modifier.padding(top = 5.dp, bottom = 10.dp)
                            .fillMaxWidth()
                            .padding(end = 16.dp),
                    singleLine = true,
                    keyboardOptions =
                        KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            imeAction = ImeAction.Done,
                        ),
                    keyboardActions = KeyboardActions(onDone = { onSubmit(stateValue) }),
                    trailing = {
                        if (!isError) {
                            IconButton(
                                onClick = {
                                    onSubmit(stateValue)
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }
                            ) {
                                val icon = Icons.Outlined.Save
                                Icon(
                                    imageVector = icon,
                                    contentDescription = icon.name,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    },
                )
            }
        },
    )
}
