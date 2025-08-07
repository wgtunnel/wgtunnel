package com.zaneschepke.wireguardautotunnel.ui.common.config

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.ui.common.textbox.CustomTextField

@Composable
fun SubmitConfigurationTextBox(
    value: String?,
    label: String,
    hint: String,
    isErrorValue: (value: String?) -> Boolean,
    onSubmit: (value: String) -> Unit,
    supportingText: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions =
        KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Done),
) {
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    var stateValue by remember { mutableStateOf(value ?: "") }

    CustomTextField(
        isError = isErrorValue(stateValue),
        textStyle =
            MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
        value = stateValue,
        onValueChange = { stateValue = it },
        interactionSource = interactionSource,
        supportingText = supportingText,
        label = {
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelMedium,
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        placeholder = {
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        },
        modifier = Modifier.padding(top = 5.dp, bottom = 10.dp).fillMaxWidth().padding(end = 16.dp),
        singleLine = true,
        keyboardOptions = keyboardOptions,
        keyboardActions =
            KeyboardActions(
                onDone = {
                    onSubmit(stateValue)
                    keyboardController?.hide()
                }
            ),
        trailing = {
            if (!isErrorValue(stateValue) && isFocused) {
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
