package com.zaneschepke.wireguardautotunnel.ui.common.textbox

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTextField(
    value: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle =
        MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
    label: @Composable () -> Unit,
    containerColor: Color,
    onValueChange: (value: String) -> Unit = {},
    singleLine: Boolean = true,
    placeholder: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(),
    keyboardActions: KeyboardActions = KeyboardActions(),
    supportingText: @Composable (() -> Unit)? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = MutableInteractionSource(),
) {
    val space = " "
    var isFocused by remember { mutableStateOf(false) }
    val cursorBrush =
        if (isFocused) SolidColor(MaterialTheme.colorScheme.primary)
        else SolidColor(Color.Transparent)

    BasicTextField(
        value = value,
        textStyle = textStyle,
        onValueChange = { onValueChange(it) },
        keyboardActions = keyboardActions,
        keyboardOptions = keyboardOptions,
        readOnly = readOnly,
        cursorBrush = cursorBrush,
        modifier = modifier.onFocusChanged { focusState -> isFocused = focusState.isFocused },
        interactionSource = interactionSource,
        enabled = enabled,
        singleLine = singleLine,
        visualTransformation = visualTransformation,
    ) {
        OutlinedTextFieldDefaults.DecorationBox(
            value = space + value,
            innerTextField = {
                if (value.isEmpty()) {
                    if (placeholder != null) {
                        placeholder()
                    }
                }
                it.invoke()
            },
            contentPadding = OutlinedTextFieldDefaults.contentPadding(top = 0.dp, bottom = 0.dp),
            leadingIcon = leading,
            trailingIcon = trailing,
            singleLine = singleLine,
            supportingText = supportingText,
            colors =
                TextFieldDefaults.colors()
                    .copy(
                        disabledLabelColor = MaterialTheme.colorScheme.onSurface,
                        disabledContainerColor = containerColor,
                        focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = containerColor,
                        unfocusedContainerColor = containerColor,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                    ),
            enabled = enabled,
            label = label,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            placeholder = placeholder,
            container = {
                OutlinedTextFieldDefaults.Container(
                    enabled = enabled,
                    isError = isError,
                    interactionSource = interactionSource,
                    colors =
                        TextFieldDefaults.colors()
                            .copy(
                                errorContainerColor = containerColor,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurface,
                                disabledContainerColor = containerColor,
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                                focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                                focusedContainerColor = containerColor,
                                unfocusedContainerColor = containerColor,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                cursorColor = MaterialTheme.colorScheme.primary,
                            ),
                    shape = RoundedCornerShape(8.dp),
                    focusedBorderThickness = 0.5.dp,
                    unfocusedBorderThickness = 0.5.dp,
                )
            },
        )
    }
}
