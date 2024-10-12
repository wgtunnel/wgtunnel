package com.zaneschepke.wireguardautotunnel.ui.common.config

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.text.isDigitsOnly
import com.zaneschepke.wireguardautotunnel.R

@Composable
fun SubmitConfigurationTextBox(
	value: String?,
	label: String,
	hint: String,
	focusRequester: FocusRequester,
	isErrorValue: (value: String?) -> Boolean,
	onSubmit: (value: String) -> Unit,
	keyboardOptions: KeyboardOptions = KeyboardOptions(
		capitalization = KeyboardCapitalization.None,
		imeAction = ImeAction.Done,
	),
) {
	val focusManager = LocalFocusManager.current
	val interactionSource = remember { MutableInteractionSource() }
	val isFocused by interactionSource.collectIsFocusedAsState()
	val keyboardController = LocalSoftwareKeyboardController.current

	var stateValue by remember { mutableStateOf(value) }

	ConfigurationTextBox(
		isError = isErrorValue(stateValue),
		interactionSource = interactionSource,
		value = stateValue ?: "",
		onValueChange = {
			when (keyboardOptions.keyboardType) {
				KeyboardType.Number -> {
					if (it.isDigitsOnly()) stateValue = it
				}
				else -> stateValue = it
			}
		},
		keyboardOptions = keyboardOptions,
		label = label,
		keyboardActions = KeyboardActions(
			onDone = {
				onSubmit(stateValue!!)
				keyboardController?.hide()
			},
		),
		hint = hint,
		modifier = Modifier
			.fillMaxWidth()
			.focusRequester(focusRequester),
		trailing = {
			if (!stateValue.isNullOrBlank() && !isErrorValue(stateValue) && isFocused) {
				IconButton(onClick = {
					onSubmit(stateValue!!)
					keyboardController?.hide()
					focusManager.clearFocus()
				}) {
					Icon(
						imageVector = Icons.Outlined.Save,
						contentDescription = stringResource(R.string.save_changes),
						tint = MaterialTheme.colorScheme.primary,
					)
				}
			}
		},
	)
}
