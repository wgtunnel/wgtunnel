package com.zaneschepke.wireguardautotunnel.ui.screens.main.config.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.config.ConfigurationTextBox
import com.zaneschepke.wireguardautotunnel.ui.common.functions.rememberClipboardHelper
import com.zaneschepke.wireguardautotunnel.ui.state.InterfaceProxy

@Composable
fun InterfaceFields(
    interfaceState: InterfaceProxy,
    showAuthPrompt: () -> Unit,
    isAuthenticated: Boolean,
    showScripts: Boolean,
    showAmneziaValues: Boolean,
    onInterfaceChange: (InterfaceProxy) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = rememberClipboardHelper()
    val keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
    val keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)

    OutlinedTextField(
        value = interfaceState.privateKey,
        onValueChange = { onInterfaceChange(interfaceState.copy(privateKey = it)) },
        label = {
            Text(stringResource(R.string.private_key), style = MaterialTheme.typography.bodyMedium)
        },
        modifier = Modifier.fillMaxWidth().clickable { if (!isAuthenticated) showAuthPrompt() },
        visualTransformation =
            if (isAuthenticated) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(
                enabled = true,
                onClick = {
                    if (!isAuthenticated) return@IconButton showAuthPrompt()
                    val keypair = com.wireguard.crypto.KeyPair()
                    onInterfaceChange(
                        interfaceState.copy(
                            privateKey = keypair.privateKey.toBase64(),
                            publicKey = keypair.publicKey.toBase64(),
                        )
                    )
                },
            ) {
                Icon(
                    Icons.Rounded.Refresh,
                    stringResource(R.string.rotate_keys),
                    tint =
                        if (isAuthenticated) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.outline,
                )
            }
        },
        enabled = isAuthenticated,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
    )
    OutlinedTextField(
        value = interfaceState.publicKey,
        onValueChange = { onInterfaceChange(interfaceState.copy(publicKey = it)) },
        label = {
            Text(stringResource(R.string.public_key), style = MaterialTheme.typography.bodyMedium)
        },
        enabled = false,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = { clipboardManager.copy(interfaceState.publicKey) }) {
                Icon(Icons.Rounded.ContentCopy, stringResource(R.string.copy_public_key))
            }
        },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
    )
    ConfigurationTextBox(
        value = interfaceState.addresses,
        onValueChange = { onInterfaceChange(interfaceState.copy(addresses = it)) },
        label = stringResource(R.string.addresses),
        hint = stringResource(R.string.comma_separated_list),
        modifier = Modifier.fillMaxWidth(),
    )
    ConfigurationTextBox(
        value = interfaceState.listenPort,
        onValueChange = { onInterfaceChange(interfaceState.copy(listenPort = it)) },
        label = stringResource(R.string.listen_port),
        hint = stringResource(R.string.random),
        modifier = Modifier.fillMaxWidth(),
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ConfigurationTextBox(
            value = interfaceState.dnsServers,
            onValueChange = { onInterfaceChange(interfaceState.copy(dnsServers = it)) },
            label = stringResource(R.string.dns_servers),
            hint = stringResource(R.string.comma_separated_list),
            modifier = Modifier.weight(3f),
        )
        ConfigurationTextBox(
            value = interfaceState.mtu,
            onValueChange = { onInterfaceChange(interfaceState.copy(mtu = it)) },
            label = stringResource(R.string.mtu),
            hint = stringResource(R.string.auto),
            modifier = Modifier.weight(2f),
        )
    }
    if (showScripts) {
        ConfigurationTextBox(
            value = interfaceState.preUp,
            onValueChange = { onInterfaceChange(interfaceState.copy(preUp = it)) },
            label = stringResource(R.string.pre_up),
            hint = stringResource(R.string.comma_separated_list).lowercase(),
            modifier = Modifier.fillMaxWidth(),
        )
        ConfigurationTextBox(
            value = interfaceState.postUp,
            onValueChange = { onInterfaceChange(interfaceState.copy(postUp = it)) },
            label = stringResource(R.string.post_up),
            hint = stringResource(R.string.comma_separated_list).lowercase(),
            modifier = Modifier.fillMaxWidth(),
        )
        ConfigurationTextBox(
            value = interfaceState.preDown,
            onValueChange = { onInterfaceChange(interfaceState.copy(preDown = it)) },
            label = stringResource(R.string.pre_down),
            hint = stringResource(R.string.comma_separated_list).lowercase(),
            modifier = Modifier.fillMaxWidth(),
        )
        ConfigurationTextBox(
            value = interfaceState.postDown,
            onValueChange = { onInterfaceChange(interfaceState.copy(postDown = it)) },
            label = stringResource(R.string.post_down),
            hint = stringResource(R.string.comma_separated_list).lowercase(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
    if (showAmneziaValues) {
        ConfigurationTextBox(
            value = interfaceState.junkPacketCount,
            onValueChange = { onInterfaceChange(interfaceState.copy(junkPacketCount = it)) },
            label = stringResource(R.string.junk_packet_count),
            hint = stringResource(R.string.junk_packet_count).lowercase(),
            modifier = Modifier.fillMaxWidth(),
        )
        ConfigurationTextBox(
            value = interfaceState.junkPacketMinSize,
            onValueChange = { onInterfaceChange(interfaceState.copy(junkPacketMinSize = it)) },
            label = stringResource(R.string.junk_packet_minimum_size),
            hint = stringResource(R.string.junk_packet_minimum_size).lowercase(),
            modifier = Modifier.fillMaxWidth(),
        )
        ConfigurationTextBox(
            value = interfaceState.junkPacketMaxSize,
            onValueChange = { onInterfaceChange(interfaceState.copy(junkPacketMaxSize = it)) },
            label = stringResource(R.string.junk_packet_maximum_size),
            hint = stringResource(R.string.junk_packet_maximum_size).lowercase(),
            modifier = Modifier.fillMaxWidth(),
        )
        ConfigurationTextBox(
            value = interfaceState.initPacketJunkSize,
            onValueChange = { onInterfaceChange(interfaceState.copy(initPacketJunkSize = it)) },
            label = stringResource(R.string.init_packet_junk_size),
            hint = stringResource(R.string.init_packet_junk_size).lowercase(),
            modifier = Modifier.fillMaxWidth(),
        )
        ConfigurationTextBox(
            value = interfaceState.responsePacketJunkSize,
            onValueChange = { onInterfaceChange(interfaceState.copy(responsePacketJunkSize = it)) },
            label = stringResource(R.string.response_packet_junk_size),
            hint = stringResource(R.string.response_packet_junk_size).lowercase(),
            modifier = Modifier.fillMaxWidth(),
        )
        ConfigurationTextBox(
            value = interfaceState.initPacketMagicHeader,
            onValueChange = { onInterfaceChange(interfaceState.copy(initPacketMagicHeader = it)) },
            label = stringResource(R.string.init_packet_magic_header),
            hint = stringResource(R.string.init_packet_magic_header).lowercase(),
            modifier = Modifier.fillMaxWidth(),
        )
        ConfigurationTextBox(
            value = interfaceState.responsePacketMagicHeader,
            onValueChange = {
                onInterfaceChange(interfaceState.copy(responsePacketMagicHeader = it))
            },
            label = stringResource(R.string.response_packet_magic_header),
            hint = stringResource(R.string.response_packet_magic_header).lowercase(),
            modifier = Modifier.fillMaxWidth(),
        )
        ConfigurationTextBox(
            value = interfaceState.underloadPacketMagicHeader,
            onValueChange = {
                onInterfaceChange(interfaceState.copy(underloadPacketMagicHeader = it))
            },
            label = stringResource(R.string.underload_packet_magic_header),
            hint = stringResource(R.string.underload_packet_magic_header).lowercase(),
            modifier = Modifier.fillMaxWidth(),
        )
        ConfigurationTextBox(
            value = interfaceState.transportPacketMagicHeader,
            onValueChange = {
                onInterfaceChange(interfaceState.copy(transportPacketMagicHeader = it))
            },
            label = stringResource(R.string.transport_packet_magic_header),
            hint = stringResource(R.string.transport_packet_magic_header).lowercase(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
