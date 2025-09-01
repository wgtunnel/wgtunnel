package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState
import com.zaneschepke.wireguardautotunnel.util.NumberUtils
import com.zaneschepke.wireguardautotunnel.util.extensions.toThreeDecimalPlaceString

@Composable
fun TunnelStatisticsRow(
    tunnelState: TunnelState,
    tunnelConf: TunnelConf,
    pingEnabled: Boolean,
    showDetailedStats: Boolean,
) {
    val config = remember(tunnelConf) { TunnelConf.configFromAmQuick(tunnelConf.wgQuick) }
    val peerText = stringResource(R.string.peer)
    val handshakeText = stringResource(R.string.handshake)
    val endpointText = stringResource(R.string.endpoint)
    val neverText = stringResource(R.string.never)
    val textStyle = MaterialTheme.typography.bodySmall
    val textColor = MaterialTheme.colorScheme.outline

    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 45.dp, bottom = 10.dp, end = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        config.peers.forEachIndexed { index, peer ->
            key(peer.publicKey.toBase64()) { // Key by peer ID to skip recomposition if unchanged
                val peerStats =
                    remember(tunnelState.statistics, peer, tunnelConf) {
                        tunnelState.statistics?.peerStats(peer.publicKey)
                    }
                val peerId =
                    remember(peer) {
                        peer.publicKey.toBase64().subSequence(0, 3).toString() + "***"
                    }
                val endpoint by
                    remember(peerStats) { derivedStateOf { peerStats?.resolvedEndpoint } }
                val peerRxMB by
                    remember(peerStats) {
                        derivedStateOf {
                            peerStats
                                ?.rxBytes
                                ?.let { NumberUtils.bytesToMB(it) }
                                ?.toThreeDecimalPlaceString() ?: "0.00"
                        }
                    }
                val peerTxMB by
                    remember(peerStats) {
                        derivedStateOf {
                            peerStats
                                ?.txBytes
                                ?.let { NumberUtils.bytesToMB(it) }
                                ?.toThreeDecimalPlaceString() ?: "0.00"
                        }
                    }
                val handshake by
                    remember(peerStats) {
                        derivedStateOf {
                            peerStats?.latestHandshakeEpochMillis?.let {
                                if (it == 0L) null
                                else NumberUtils.getSecondsBetweenTimestampAndNow(it).toString()
                            }
                        }
                    }
                val pingState by
                    remember(tunnelState.pingStates) {
                        derivedStateOf {
                            tunnelState.pingStates?.getOrDefault(peer.publicKey, null)
                        }
                    }
                val lastPingedSeconds by
                    remember(peerStats) {
                        derivedStateOf {
                            pingState?.lastSuccessfulPingMillis?.let {
                                NumberUtils.getSecondsBetweenTimestampAndNow(it)
                            }
                        }
                    }

                // Group peer stats in a column with internal spacing
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text("$peerText: $peerId", style = textStyle, color = textColor)
                        Text(
                            "$handshakeText: ${handshake?.let { stringResource(R.string.sec_ago_template, it)} ?: neverText}",
                            style = textStyle,
                            color = textColor,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            stringResource(R.string.rx_template, peerRxMB),
                            style = textStyle,
                            color = textColor,
                        )
                        Text(
                            stringResource(R.string.tx_template, peerTxMB),
                            style = textStyle,
                            color = textColor,
                        )
                    }
                    AnimatedVisibility(visible = endpoint != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text("$endpointText: $endpoint", style = textStyle, color = textColor)
                        }
                    }
                    AnimatedVisibility(visible = pingState != null && pingEnabled) {
                        pingState?.let {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    Text(
                                        stringResource(
                                            R.string.reachable_template,
                                            stringResource(
                                                if (it.isReachable) R.string._true
                                                else R.string._false
                                            ),
                                        ),
                                        style = textStyle,
                                        color = textColor,
                                    )
                                    Text(
                                        stringResource(
                                            R.string.ping_target_template,
                                            it.pingTarget,
                                        ),
                                        style = textStyle,
                                        color = textColor,
                                    )
                                }
                                if (showDetailedStats) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    ) {
                                        Text(
                                            stringResource(R.string.latency_template, it.rttAvg),
                                            style = textStyle,
                                            color = textColor,
                                        )
                                        Text(
                                            stringResource(R.string.jitter_template, it.rttStddev),
                                            style = textStyle,
                                            color = textColor,
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    ) {
                                        Text(
                                            stringResource(
                                                R.string.packets_sent_template,
                                                it.transmitted,
                                            ),
                                            style = textStyle,
                                            color = textColor,
                                        )
                                        Text(
                                            stringResource(
                                                R.string.packet_loss_template,
                                                it.packetLoss,
                                            ),
                                            style = textStyle,
                                            color = textColor,
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    ) {
                                        Text(
                                            stringResource(
                                                R.string.ping_success_template,
                                                lastPingedSeconds?.let { sec ->
                                                    stringResource(R.string.sec_ago_template, sec)
                                                } ?: neverText,
                                            ),
                                            style = textStyle,
                                            color = textColor,
                                        )
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
