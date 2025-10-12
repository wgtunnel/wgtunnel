package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.tunneloptions

import android.util.Patterns
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.LocalSharedVm
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.ui.common.text.DescriptionText
import com.zaneschepke.wireguardautotunnel.ui.common.textbox.CustomTextField
import com.zaneschepke.wireguardautotunnel.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.tunneloptions.components.*
import com.zaneschepke.wireguardautotunnel.ui.sideeffect.LocalSideEffect
import com.zaneschepke.wireguardautotunnel.util.extensions.isValidIpv4orIpv6Address
import com.zaneschepke.wireguardautotunnel.viewmodel.TunnelViewModel
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun TunnelOptionsScreen(viewModel: TunnelViewModel) {
    val navController = LocalNavController.current
    val sharedViewModel = LocalSharedVm.current

    val tunnelState by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    if (tunnelState.isLoading) return
    val tunnel = tunnelState.tunnel ?: return

    var showQrModal by rememberSaveable { mutableStateOf(false) }

    sharedViewModel.collectSideEffect { sideEffect ->
        if (sideEffect is LocalSideEffect.Modal.QR) showQrModal = true
    }

    if (showQrModal) {
        QrCodeDialog(tunnelConf = tunnel, onDismiss = { showQrModal = false })
    }

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        Column {
            SurfaceRow(
                leading = { Icon(Icons.Outlined.Star, contentDescription = null) },
                title = stringResource(R.string.primary_tunnel),
                description = {
                    Text(
                        text = stringResource(R.string.set_primary_tunnel),
                        style =
                            MaterialTheme.typography.bodySmall.copy(
                                MaterialTheme.colorScheme.outline
                            ),
                    )
                },
                trailing = {
                    ScaledSwitch(
                        checked = tunnel.isPrimaryTunnel,
                        onClick = { viewModel.togglePrimaryTunnel() },
                    )
                },
                onClick = { viewModel.togglePrimaryTunnel() },
            )
            SurfaceRow(
                leading = {
                    Icon(Icons.AutoMirrored.Outlined.CallSplit, contentDescription = null)
                },
                title = stringResource(R.string.splt_tunneling),
                onClick = { navController.push(Route.SplitTunnel(id = tunnel.id)) },
            )
        }
        Column {
            SurfaceRow(
                leading = { Icon(Icons.Outlined.Dns, contentDescription = null) },
                title = stringResource(R.string.ddns_auto_update),
                description = {
                    DescriptionText(stringResource(R.string.ddns_auto_update_description))
                },
                trailing = {
                    ScaledSwitch(
                        checked = tunnel.restartOnPingFailure,
                        onClick = { viewModel.setRestartOnPing(it) },
                    )
                },
                onClick = { viewModel.setRestartOnPing(!tunnel.restartOnPingFailure) },
            )
            SurfaceRow(
                leading = {
                    Icon(ImageVector.vectorResource(R.drawable.host), contentDescription = null)
                },
                title = stringResource(R.string.prefer_ipv6_resolution),
                trailing = {
                    ScaledSwitch(
                        checked = !tunnel.isIpv4Preferred,
                        onClick = { viewModel.toggleIpv4Preferred() },
                    )
                },
                onClick = { viewModel.toggleIpv4Preferred() },
            )
            SurfaceRow(
                title = "",
                description = {
                    val focusManager = LocalFocusManager.current
                    val keyboardController = LocalSoftwareKeyboardController.current

                    var stateValue by remember { mutableStateOf(tunnel.pingTarget ?: "") }
                    val isError by
                        remember(stateValue) {
                            derivedStateOf {
                                stateValue.isNotBlank() &&
                                    !stateValue.isValidIpv4orIpv6Address() &&
                                    !Patterns.DOMAIN_NAME.matcher(stateValue).matches()
                            }
                        }
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CustomTextField(
                            isError = isError,
                            textStyle =
                                MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                            value = stateValue,
                            onValueChange = { stateValue = it },
                            interactionSource = remember { MutableInteractionSource() },
                            label = {
                                Text(
                                    stringResource(R.string.set_custom_ping_target),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            },
                            placeholder = {
                                Text(
                                    stringResource(R.string.ip_or_hostname),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            },
                            containerColor = MaterialTheme.colorScheme.surface,
                            supportingText = {
                                Text(stringResource(R.string.ping_target_description))
                            },
                            modifier =
                                Modifier.padding(top = 5.dp, bottom = 10.dp)
                                    .fillMaxWidth()
                                    .padding(end = 16.dp),
                            singleLine = true,
                            keyboardOptions =
                                KeyboardOptions(
                                    capitalization = KeyboardCapitalization.None,
                                    imeAction = ImeAction.Done,
                                ),
                            keyboardActions =
                                KeyboardActions(onDone = { viewModel.setTunnelPingIp(stateValue) }),
                            trailing = {
                                if (!isError) {
                                    IconButton(
                                        onClick = {
                                            viewModel.setTunnelPingIp(stateValue)
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
                },
            )
        }
    }
}
