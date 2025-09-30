package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.config.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
fun PeerFields(isGlobalConfig: Boolean, peer: PeerProxy, onPeerChange: (PeerProxy) -> Unit) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
    val keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
    var showPresharedKey by rememberSaveable { mutableStateOf(false) }

    if(!isGlobalConfig)ConfigurationTextBox(
        value = peer.publicKey,
        onValueChange = { onPeerChange(peer.copy(publicKey = it)) },
        label = stringResource(R.string.public_key),
        hint =
            stringResource(R.string.hint_template, stringResource(R.string.base64_key))
                .lowercase(Locale.getDefault()),
        modifier = Modifier.fillMaxWidth(),
    )
    if(!isGlobalConfig) ConfigurationTextBox(
        visualTransformation =
            if (showPresharedKey) VisualTransformation.None else PasswordVisualTransformation(),
        value = peer.preSharedKey,
        enabled = true,
        hint = stringResource(R.string.optional),
        onValueChange = { onPeerChange(peer.copy(preSharedKey = it)) },
        label = stringResource(R.string.preshared_key),
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = true,
        trailing = {
            IconButton(onClick = { showPresharedKey = !showPresharedKey }) {
                Icon(Icons.Outlined.RemoveRedEye, stringResource(R.string.show_password))
            }
        },
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
    if(!isGlobalConfig) ConfigurationTextBox(
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
