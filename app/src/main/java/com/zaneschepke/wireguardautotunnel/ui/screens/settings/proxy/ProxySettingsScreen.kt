import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forward5
import androidx.compose.material.icons.outlined.Http
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.model.AppProxySettings
import com.zaneschepke.wireguardautotunnel.ui.LocalSharedVm
import com.zaneschepke.wireguardautotunnel.ui.common.SecureScreenFromRecording
import com.zaneschepke.wireguardautotunnel.ui.common.button.ActionIconButton
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItemLabel
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionLabelType
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.ui.common.textbox.ConfigurationTextBox
import com.zaneschepke.wireguardautotunnel.ui.state.NavbarState
import com.zaneschepke.wireguardautotunnel.viewmodel.ProxySettingsViewModel

@Composable
fun ProxySettingsScreen(viewModel: ProxySettingsViewModel = hiltViewModel()) {

    val sharedViewModel = LocalSharedVm.current

    val proxySettingsState by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    val proxySettings by remember { derivedStateOf { proxySettingsState.proxySettings } }

    var socksBindAddress by remember { mutableStateOf(proxySettings.socks5ProxyBindAddress ?: "") }
    var httpBindAddress by remember { mutableStateOf(proxySettings.httpProxyBindAddress ?: "") }
    var proxyUsername by remember { mutableStateOf(proxySettings.proxyUsername ?: "") }
    var proxyPassword by remember { mutableStateOf(proxySettings.proxyPassword ?: "") }
    var passwordVisible by remember { mutableStateOf(proxySettingsState.passwordVisible) }

    val keyboardController = LocalSoftwareKeyboardController.current

    val keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
    val keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)

    if (!proxySettingsState.stateInitialized) return

    LaunchedEffect(Unit) {
        sharedViewModel.updateNavbarState(
            NavbarState(
                showBottomItems = true,
                topTitle = { Text(stringResource(R.string.proxy_settings)) },
                topTrailing = {
                    ActionIconButton(Icons.Rounded.Save, R.string.save) {
                        keyboardController?.hide()
                        viewModel.save(
                            AppProxySettings(
                                socks5ProxyEnabled = proxySettings.socks5ProxyEnabled,
                                socks5ProxyBindAddress = socksBindAddress,
                                httpProxyEnabled = proxySettings.httpProxyEnabled,
                                httpProxyBindAddress = httpBindAddress,
                                proxyUsername = proxyUsername,
                                proxyPassword = proxyPassword,
                            )
                        )
                    }
                },
            )
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
                            checked = proxySettings.socks5ProxyEnabled,
                            onClick = { viewModel.setEnableSocks5(it) },
                        )
                    },
                    onClick = { viewModel.setEnableSocks5(!proxySettings.socks5ProxyEnabled) },
                )
            )
        )
        if (proxySettings.socks5ProxyEnabled) {
            ConfigurationTextBox(
                hint =
                    stringResource(
                        R.string.defaults_to_template,
                        AppProxySettings.DEFAULT_SOCKS_BIND_ADDRESS,
                    ),
                label = stringResource(R.string.socks_5_bind_address),
                value = socksBindAddress,
                isError = proxySettingsState.isSocks5BindAddressError,
                onValueChange = { socksBindAddress = it },
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
                            checked = proxySettings.httpProxyEnabled,
                            onClick = { viewModel.setEnableHttp(it) },
                        )
                    },
                    onClick = { viewModel.setEnableHttp(!proxySettings.httpProxyEnabled) },
                )
            )
        )
        if (proxySettings.httpProxyEnabled) {
            ConfigurationTextBox(
                hint =
                    stringResource(
                        R.string.defaults_to_template,
                        AppProxySettings.DEFAULT_HTTP_BIND_ADDRESS,
                    ),
                label = stringResource(R.string.http_bind_address),
                value = httpBindAddress,
                isError = proxySettingsState.isHttpBindAddressError,
                onValueChange = { httpBindAddress = it },
            )
        }
        if (proxySettings.httpProxyEnabled || proxySettings.socks5ProxyEnabled) {
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
                    value = proxyUsername,
                    onValueChange = { proxyUsername = it },
                    label = stringResource(R.string.username),
                    isError = proxySettingsState.isUserNameError,
                    hint = "",
                    keyboardActions = keyboardActions,
                    keyboardOptions = keyboardOptions,
                    modifier = Modifier.fillMaxWidth(),
                )
                ConfigurationTextBox(
                    value = proxyPassword,
                    onValueChange = { proxyPassword = it },
                    label = stringResource(R.string.password),
                    isError = proxySettingsState.isPasswordError,
                    hint = "",
                    keyboardActions = keyboardActions,
                    keyboardOptions = keyboardOptions,
                    modifier = Modifier.fillMaxWidth(),
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
