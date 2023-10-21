package com.zaneschepke.wireguardautotunnel.ui.screens.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.util.NumberUtils
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel = hiltViewModel(),
    focusRequester: FocusRequester,
    padding: PaddingValues,
    id : String
) {

    val context = LocalContext.current
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val tunnelStats by viewModel.tunnelStats.collectAsStateWithLifecycle(null)
    val tunnel by viewModel.tunnel.collectAsStateWithLifecycle(null)
    val tunnelName by viewModel.tunnelName.collectAsStateWithLifecycle()
    val lastHandshake by viewModel.lastHandshake.collectAsStateWithLifecycle(emptyMap())


    LaunchedEffect(Unit) {
        viewModel.emitConfig(id)
    }

    if(null != tunnel) {
        val interfaceKey = tunnel?.`interface`?.keyPair?.publicKey?.toBase64().toString()
        val addresses = tunnel?.`interface`?.addresses!!.joinToString()
        val dnsServers = tunnel?.`interface`?.dnsServers!!.joinToString()
        val optionalMtu = tunnel?.`interface`?.mtu
        val mtu = if(optionalMtu?.isPresent == true) optionalMtu.get().toString() else stringResource(
            id = R.string.none
        )
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(if(WireGuardAutoTunnel.isRunningOnAndroidTv(context)) 4/5f else 1f)
                .verticalScroll(rememberScrollState())
                .focusRequester(focusRequester)
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 7.dp).focusGroup(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f, true)) {
                    Text(stringResource(R.string.config_interface), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(stringResource(R.string.name), fontStyle = FontStyle.Italic)
                    Text(text = tunnelName,  modifier = Modifier.clickable {
                        clipboardManager.setText(AnnotatedString(tunnelName))
                    })
                    Text(stringResource(R.string.public_key), fontStyle = FontStyle.Italic)
                    Text(text = interfaceKey,  modifier = Modifier.clickable {
                        clipboardManager.setText(AnnotatedString(interfaceKey))
                    })
                    Text(stringResource(R.string.addresses), fontStyle = FontStyle.Italic)
                    Text(text = addresses,  modifier = Modifier.clickable {
                        clipboardManager.setText(AnnotatedString(addresses))
                    })
                    Text(stringResource(R.string.dns_servers), fontStyle = FontStyle.Italic)
                    Text(text = dnsServers,  modifier = Modifier.clickable {
                        clipboardManager.setText(AnnotatedString(dnsServers))
                    })
                    Text(stringResource(R.string.mtu), fontStyle = FontStyle.Italic)
                    Text(text = mtu,  modifier = Modifier.clickable {
                        clipboardManager.setText(AnnotatedString(mtu))
                    })
                    Box(modifier = Modifier.padding(10.dp))
                    tunnel?.peers?.forEach{
                        val peerKey = it.publicKey.toBase64()
                        val allowedIps = it.allowedIps.joinToString()
                        val endpoint = if(it.endpoint.isPresent) it.endpoint.get().toString() else stringResource(
                            id = R.string.none
                        )
                        Text(stringResource(R.string.peer), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(stringResource(R.string.public_key), fontStyle = FontStyle.Italic)
                        Text(text = peerKey,  modifier = Modifier.clickable {
                            clipboardManager.setText(AnnotatedString(peerKey))
                        })
                        Text(stringResource(id = R.string.allowed_ips), fontStyle = FontStyle.Italic)
                        Text(text = allowedIps,  modifier = Modifier.clickable {
                            clipboardManager.setText(AnnotatedString(allowedIps))
                        })
                        Text(stringResource(R.string.endpoint), fontStyle = FontStyle.Italic)
                        Text(text = endpoint,  modifier = Modifier.clickable {
                            clipboardManager.setText(AnnotatedString(endpoint))
                        })
                        if (tunnelStats != null) {
                            val totalRx = tunnelStats?.totalRx() ?: 0
                            val totalTx = tunnelStats?.totalTx() ?: 0
                            if((totalRx + totalTx != 0L))  {
                                val rxKB = NumberUtils.bytesToKB(tunnelStats!!.totalRx())
                                val txKB = NumberUtils.bytesToKB(tunnelStats!!.totalTx())
                                Text(stringResource(R.string.transfer), fontStyle = FontStyle.Italic)
                                val transfer = "rx: ${NumberUtils.formatDecimalTwoPlaces(rxKB)} KB tx: ${NumberUtils.formatDecimalTwoPlaces(txKB)} KB"
                                Text(transfer, modifier = Modifier.clickable {
                                    clipboardManager.setText(AnnotatedString(transfer))})
                                Text(stringResource(R.string.last_handshake), fontStyle = FontStyle.Italic)
                                val handshakeEpoch = lastHandshake[it.publicKey]
                                if(handshakeEpoch != null) {
                                    if(handshakeEpoch == 0L) {
                                        Text(stringResource(id = R.string.never), modifier = Modifier.clickable {
                                            clipboardManager.setText(AnnotatedString(context.getString(R.string.never)))
                                        })
                                    } else {
                                        val time = Instant.ofEpochMilli(handshakeEpoch)
                                        val duration = "${Duration.between(time, Instant.now()).seconds} seconds ago"
                                        Text(duration, modifier = Modifier.clickable {
                                            clipboardManager.setText(AnnotatedString(duration))
                                        })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}