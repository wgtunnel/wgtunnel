package com.zaneschepke.wireguardautotunnel.ui.screens.settings.backend.credentials

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.SecureScreenFromRecording
import com.zaneschepke.wireguardautotunnel.ui.common.config.ConfigurationTextBox
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.util.StringValue
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun ProxyCredentialsScreen(uiState : AppUiState, viewModel: AppViewModel) {

    val keyboardController = LocalSoftwareKeyboardController.current
    val keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
    val keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)

    SecureScreenFromRecording()

    var showPassword by remember { mutableStateOf(false) }
    var usernameError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf(uiState.appSettings.proxyUsername ?: "") }
    var password by remember { mutableStateOf(uiState.appSettings.proxyPassword ?: "") }

    LaunchedEffect(Unit) {
        viewModel.handleEvent(AppEvent.SetScreenAction {
            if (username.isBlank() && password.isNotBlank()) {
                usernameError = true
                return@SetScreenAction
            }
            if (password.isBlank() && username.isNotBlank()) {
                passwordError = true
                return@SetScreenAction
            }
            keyboardController?.hide()
            viewModel.handleEvent(AppEvent.SetProxyCredentials(username, password))
            viewModel.handleEvent(AppEvent.ShowMessage(StringValue.StringResource(R.string.config_changes_saved)))
            viewModel.handleEvent(AppEvent.PopBackStack(true))
        })
    }

    LaunchedEffect(username) {
        if(username.isNotBlank() && usernameError) usernameError = false
    }

    LaunchedEffect(password) {
        if(password.isNotBlank() && passwordError) passwordError = false
    }

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
        modifier = Modifier.fillMaxSize().padding(top = 24.dp).padding(horizontal = 24.dp).clickable {
            keyboardController?.hide()
        },
    ) {
        ConfigurationTextBox(
            value = username,
            onValueChange = { username = it } ,
            label = stringResource(R.string.username),
            isError = usernameError,
            hint = "",
            keyboardActions = keyboardActions,
            keyboardOptions = keyboardOptions,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            visualTransformation =
                if (showPassword || password.isBlank()) VisualTransformation.None else PasswordVisualTransformation(),
            value = password,
            enabled = showPassword,
            onValueChange = { password = it },
            label = {
                Text(
                    stringResource(R.string.password),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            modifier = Modifier.fillMaxWidth().clickable { showPassword = true },
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = true,
        )
    }
}