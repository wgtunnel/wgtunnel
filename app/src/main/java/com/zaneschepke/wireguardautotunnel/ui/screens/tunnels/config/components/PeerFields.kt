package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.config.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.textbox.ConfigurationTextBox
import com.zaneschepke.wireguardautotunnel.ui.state.PeerProxy
import java.util.*

@Composable
fun PeerFields(
    peer: PeerProxy,
    onPeerChange: (PeerProxy) -> Unit,
    showAuthPrompt: () -> Unit,
    isAuthenticated: Boolean,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
    val keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)

    ConfigurationTextBox(
        value = peer.publicKey,
        onValueChange = { onPeerChange(peer.copy(publicKey = it)) },
        label = stringResource(R.string.public_key),
        hint =
            stringResource(R.string.hint_template, stringResource(R.string.base64_key))
                .lowercase(Locale.getDefault()),
        modifier = Modifier.fillMaxWidth(),
    )
    ConfigurationTextBox(
        visualTransformation =
            if (isAuthenticated) VisualTransformation.None else PasswordVisualTransformation(),
        value = peer.preSharedKey,
        enabled = isAuthenticated,
        hint = stringResource(R.string.optional),
        onValueChange = { onPeerChange(peer.copy(preSharedKey = it)) },
        label = stringResource(R.string.preshared_key),
        modifier = Modifier.fillMaxWidth().clickable { if (!isAuthenticated) showAuthPrompt() },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = true,
    )
    ConfigurationTextBox(
        value = peer.persistentKeepalive,
        onValueChange = { onPeerChange(peer.copy(persistentKeepalive = it)) },
        label = stringResource(R.string.persistent_keepalive),
        hint = stringResource(R.string.optional),
        trailing = {
            Text(
                stringResource(R.string.seconds).lowercase(Locale.getDefault()),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 10.dp),
            )
        },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
    )
    ConfigurationTextBox(
        value = peer.endpoint,
        onValueChange = { onPeerChange(peer.copy(endpoint = it)) },
        label = stringResource(R.string.endpoint),
        hint =
            stringResource(R.string.hint_template, stringResource(R.string.server_port))
                .lowercase(Locale.getDefault()),
        modifier = Modifier.fillMaxWidth(),
    )
    ConfigurationTextBox(
        value = peer.allowedIps,
        onValueChange = { onPeerChange(peer.copy(allowedIps = it)) },
        label = stringResource(R.string.allowed_ips),
        hint =
            stringResource(R.string.hint_template, stringResource(R.string.comma_separated))
                .lowercase(Locale.getDefault()),
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
    )
}
