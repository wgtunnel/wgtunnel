import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forward5
import androidx.compose.material.icons.outlined.Http
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.model.AppProxySettings
import com.zaneschepke.wireguardautotunnel.ui.LocalSharedVm
import com.zaneschepke.wireguardautotunnel.ui.common.SecureScreenFromRecording
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItemLabel
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionLabelType
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.ui.common.textbox.ConfigurationTextBox
import com.zaneschepke.wireguardautotunnel.ui.sideeffect.LocalSideEffect
import com.zaneschepke.wireguardautotunnel.viewmodel.ProxySettingsViewModel
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun ProxySettingsScreen(viewModel: ProxySettingsViewModel = hiltViewModel()) {
    val sharedViewModel = LocalSharedVm.current
    val proxySettingsState by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    if (!proxySettingsState.stateInitialized) return

    val proxySettings by
        remember(proxySettingsState) { mutableStateOf(proxySettingsState.proxySettings) }

    var socks5Enabled by
        remember(proxySettings) {
            mutableStateOf(proxySettingsState.proxySettings.socks5ProxyEnabled)
        }
    var httpEnabled by
        remember(proxySettings) {
            mutableStateOf(proxySettingsState.proxySettings.httpProxyEnabled)
        }
    var socksBindAddress by
        remember(proxySettings) {
            mutableStateOf(proxySettingsState.proxySettings.socks5ProxyBindAddress ?: "")
        }
    var httpBindAddress by
        remember(proxySettings) {
            mutableStateOf(proxySettingsState.proxySettings.httpProxyBindAddress ?: "")
        }
    var proxyUsername by
        remember(proxySettings) {
            mutableStateOf(proxySettingsState.proxySettings.proxyUsername ?: "")
        }
    var proxyPassword by
        remember(proxySettings) {
            mutableStateOf(proxySettingsState.proxySettings.proxyPassword ?: "")
        }
    var passwordVisible by
        remember(proxySettings) { mutableStateOf(proxySettingsState.passwordVisible) }

    val keyboardController = LocalSoftwareKeyboardController.current

    val keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
    val keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)

    sharedViewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            LocalSideEffect.SaveChanges -> {
                viewModel.save(
                    AppProxySettings(
                        socks5ProxyEnabled = socks5Enabled,
                        socks5ProxyBindAddress = socksBindAddress,
                        httpProxyEnabled = httpEnabled,
                        httpProxyBindAddress = httpBindAddress,
                        proxyUsername = proxyUsername,
                        proxyPassword = proxyPassword,
                    )
                )
            }
            else -> Unit
        }
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
                        ScaledSwitch(checked = socks5Enabled, onClick = { socks5Enabled = it })
                    },
                    onClick = { socks5Enabled = !socks5Enabled },
                )
            )
        )
        if (socks5Enabled) {
            ConfigurationTextBox(
                modifier = Modifier.padding(horizontal = 12.dp),
                hint =
                    stringResource(
                        R.string.defaults_to_template,
                        AppProxySettings.DEFAULT_SOCKS_BIND_ADDRESS,
                    ),
                label = stringResource(R.string.socks_5_bind_address),
                value = socksBindAddress,
                isError = proxySettingsState.isSocks5BindAddressError,
                onValueChange = {
                    if (proxySettingsState.isSocks5BindAddressError)
                        viewModel.clearSocks5BindError()
                    socksBindAddress = it
                },
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
                        ScaledSwitch(checked = httpEnabled, onClick = { httpEnabled = it })
                    },
                    onClick = { httpEnabled = !httpEnabled },
                )
            )
        )
        if (httpEnabled) {
            ConfigurationTextBox(
                hint =
                    stringResource(
                        R.string.defaults_to_template,
                        AppProxySettings.DEFAULT_HTTP_BIND_ADDRESS,
                    ),
                label = stringResource(R.string.http_bind_address),
                value = httpBindAddress,
                isError = proxySettingsState.isHttpBindAddressError,
                onValueChange = {
                    if (proxySettingsState.isSocks5BindAddressError) viewModel.clearHttpBindError()
                    httpBindAddress = it
                },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        if (socks5Enabled || httpEnabled) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
            ) {
                GroupLabel(
                    stringResource(
                        R.string.recommended_template,
                        stringResource((R.string.proxy_credentials)),
                    )
                )
                ConfigurationTextBox(
                    value = proxyUsername,
                    onValueChange = {
                        if (proxySettingsState.isUserNameError) viewModel.clearUsernameError()
                        proxyUsername = it
                    },
                    label = stringResource(R.string.username),
                    isError = proxySettingsState.isUserNameError,
                    hint = "",
                    keyboardActions = keyboardActions,
                    keyboardOptions = keyboardOptions,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                ConfigurationTextBox(
                    value = proxyPassword,
                    onValueChange = {
                        if (proxySettingsState.isUserNameError) viewModel.clearPasswordError()
                        proxyPassword = it
                    },
                    label = stringResource(R.string.password),
                    isError = proxySettingsState.isPasswordError,
                    hint = "",
                    keyboardActions = keyboardActions,
                    keyboardOptions = keyboardOptions,
                    modifier = Modifier.padding(horizontal = 12.dp),
                    trailing = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                Icons.Outlined.RemoveRedEye,
                                stringResource(R.string.show_password),
                            )
                        }
                    },
                    visualTransformation =
                        if (!passwordVisible) PasswordVisualTransformation()
                        else VisualTransformation.None,
                )
            }
        }
    }
}
