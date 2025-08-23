package com.zaneschepke.wireguardautotunnel.ui.screens.settings.proxy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forward5
import androidx.compose.material.icons.outlined.Http
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.model.AppProxySettings
import com.zaneschepke.wireguardautotunnel.ui.common.SecureScreenFromRecording
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItemLabel
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionLabelType
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.ui.common.textbox.ConfigurationTextBox
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.util.StringValue
import com.zaneschepke.wireguardautotunnel.util.extensions.isValidProxyBindAddress
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun ProxySettingsScreen(uiState: AppUiState, viewModel: AppViewModel) {

    val keyboardController = LocalSoftwareKeyboardController.current

    var socks5ProxyEnabled by rememberSaveable {
        mutableStateOf(uiState.proxySettings.socks5ProxyEnabled)
    }
    var httpProxyEnabled by rememberSaveable {
        mutableStateOf(uiState.proxySettings.httpProxyEnabled)
    }

    val showAuthSettings by
        remember(httpProxyEnabled, socks5ProxyEnabled) {
            derivedStateOf { httpProxyEnabled || socks5ProxyEnabled }
        }

    var socks5bindAddress by
        rememberSaveable(uiState.proxySettings.socks5ProxyBindAddress) {
            mutableStateOf(uiState.proxySettings.socks5ProxyBindAddress)
        }
    var httpBindAddress by
        rememberSaveable(uiState.proxySettings.httpProxyBindAddress) {
            mutableStateOf(uiState.proxySettings.httpProxyBindAddress)
        }

    val isSocks5BindAddressError by
        remember(socks5bindAddress) {
            derivedStateOf {
                socks5bindAddress.let { it?.isNotBlank() == true && !it.isValidProxyBindAddress() }
            }
        }

    val isHttpBindAddressError by
        remember(httpBindAddress) {
            derivedStateOf {
                httpBindAddress.let { it?.isNotBlank() == true && !it.isValidProxyBindAddress() }
            }
        }

    var username by rememberSaveable { mutableStateOf(uiState.proxySettings.proxyUsername ?: "") }
    var password by rememberSaveable { mutableStateOf(uiState.proxySettings.proxyPassword ?: "") }

    val keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
    val keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)

    var passwordVisibility by rememberSaveable { mutableStateOf(false) }

    var usernameError by rememberSaveable { mutableStateOf(false) }
    var passwordError by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(username) { if (username.isNotBlank() && usernameError) usernameError = false }

    LaunchedEffect(password) { if (password.isNotBlank() && passwordError) passwordError = false }

    LaunchedEffect(Unit) {
        viewModel.handleEvent(
            AppEvent.SetScreenAction {
                keyboardController?.hide()
                if (username.isBlank() && password.isNotBlank()) {
                    usernameError = true
                    return@SetScreenAction
                }
                if (password.isBlank() && username.isNotBlank()) {
                    passwordError = true
                    return@SetScreenAction
                }
                if (isSocks5BindAddressError || isHttpBindAddressError) return@SetScreenAction
                keyboardController?.hide()
                viewModel.handleEvent(
                    AppEvent.SetProxySettings(
                        socks5ProxyEnabled,
                        httpProxyEnabled,
                        httpBindAddress,
                        socks5bindAddress,
                        username,
                        password,
                    )
                )
                viewModel.handleEvent(
                    AppEvent.ShowMessage(StringValue.StringResource(R.string.config_changes_saved))
                )
                viewModel.handleEvent(AppEvent.PopBackStack(true))
            }
        )
    }

    SecureScreenFromRecording()

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
        modifier = Modifier.fillMaxSize().padding(top = 24.dp).padding(horizontal = 12.dp),
    ) {
        SurfaceSelectionGroupButton(
            listOf(
                SelectionItem(
                    leading = { Icon(Icons.Outlined.Forward5, contentDescription = null) },
                    title = {
                        SelectionItemLabel(
                            stringResource(R.string.socks_5_proxy),
                            SelectionLabelType.TITLE,
                        )
                    },
                    trailing = {
                        ScaledSwitch(
                            checked = socks5ProxyEnabled,
                            onClick = { socks5ProxyEnabled = !socks5ProxyEnabled },
                        )
                    },
                    onClick = { socks5ProxyEnabled = !socks5ProxyEnabled },
                )
            )
        )
        if (socks5ProxyEnabled) {
            ConfigurationTextBox(
                hint =
                    stringResource(
                        R.string.defaults_to_template,
                        AppProxySettings.DEFAULT_SOCKS_BIND_ADDRESS,
                    ),
                label = stringResource(R.string.socks_5_bind_address),
                value = socks5bindAddress ?: "",
                isError = isSocks5BindAddressError,
                onValueChange = { socks5bindAddress = it },
            )
        }
        SurfaceSelectionGroupButton(
            listOf(
                SelectionItem(
                    leading = { Icon(Icons.Outlined.Http, contentDescription = null) },
                    title = {
                        SelectionItemLabel(
                            stringResource(R.string.http_proxy),
                            SelectionLabelType.TITLE,
                        )
                    },
                    trailing = {
                        ScaledSwitch(
                            checked = httpProxyEnabled,
                            onClick = { httpProxyEnabled = !httpProxyEnabled },
                        )
                    },
                    onClick = { httpProxyEnabled = !httpProxyEnabled },
                )
            )
        )
        if (httpProxyEnabled) {
            ConfigurationTextBox(
                hint =
                    stringResource(
                        R.string.defaults_to_template,
                        AppProxySettings.DEFAULT_HTTP_BIND_ADDRESS,
                    ),
                label = stringResource(R.string.http_bind_address),
                value = httpBindAddress ?: "",
                isError = isHttpBindAddressError,
                onValueChange = { httpBindAddress = it },
            )
        }
        if (showAuthSettings) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
                modifier = Modifier.padding(horizontal = 12.dp),
            ) {
                GroupLabel(
                    stringResource(
                        R.string.recommended_template,
                        stringResource((R.string.proxy_credentials)),
                    )
                )
                ConfigurationTextBox(
                    value = username,
                    onValueChange = { username = it },
                    label = stringResource(R.string.username),
                    isError = usernameError,
                    hint = "",
                    keyboardActions = keyboardActions,
                    keyboardOptions = keyboardOptions,
                    modifier = Modifier.fillMaxWidth(),
                )
                ConfigurationTextBox(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(R.string.password),
                    isError = passwordError,
                    hint = "",
                    keyboardActions = keyboardActions,
                    keyboardOptions = keyboardOptions,
                    modifier = Modifier.fillMaxWidth(),
                    trailing = {
                        IconButton(onClick = { passwordVisibility = !passwordVisibility }) {
                            Icon(
                                Icons.Outlined.RemoveRedEye,
                                stringResource(R.string.show_password),
                            )
                        }
                    },
                    visualTransformation =
                        if (!passwordVisibility) PasswordVisualTransformation()
                        else VisualTransformation.None,
                )
            }
        }
    }
}
