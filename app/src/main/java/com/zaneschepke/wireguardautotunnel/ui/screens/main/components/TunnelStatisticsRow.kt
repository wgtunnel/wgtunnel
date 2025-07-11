package com.zaneschepke.wireguardautotunnel.ui.screens.main.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelStatistics
import com.zaneschepke.wireguardautotunnel.util.NumberUtils
import com.zaneschepke.wireguardautotunnel.util.extensions.toThreeDecimalPlaceString

@Composable
fun TunnelStatisticsRow(statistics: TunnelStatistics?, tunnelConf: TunnelConf) {
    val config = TunnelConf.configFromAmQuick(tunnelConf.wgQuick)
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 45.dp, bottom = 10.dp, end = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.Start,
    ) {
        config.peers.forEach { peer ->
            val peerId = remember { peer.publicKey.toBase64().subSequence(0, 3).toString() + "***" }
            val endpoint =
                remember(statistics) { statistics?.peerStats(peer.publicKey)?.resolvedEndpoint }
            val peerRxMB by
                remember(statistics) {
                    derivedStateOf {
                        statistics
                            ?.peerStats(peer.publicKey)
                            ?.rxBytes
                            ?.let { NumberUtils.bytesToMB(it) }
                            ?.toThreeDecimalPlaceString()
                    }
                }
            val peerTxMB by
                remember(statistics) {
                    derivedStateOf {
                        statistics
                            ?.peerStats(peer.publicKey)
                            ?.txBytes
                            ?.let { NumberUtils.bytesToMB(it) }
                            ?.toThreeDecimalPlaceString()
                    }
                }
            val handshake by
                remember(statistics) {
                    derivedStateOf {
                        statistics?.peerStats(peer.publicKey)?.latestHandshakeEpochMillis?.let {
                            if (it == 0L) {
                                null
                            } else {
                                "${NumberUtils.getSecondsBetweenTimestampAndNow(it)}"
                            }
                        }
                    }
                }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
            ) {
                Text(
                    stringResource(R.string.peer).lowercase() + ": $peerId",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Text(
                    stringResource(R.string.handshake) +
                        ": ${if(handshake == null) stringResource(R.string.never) else handshake + " " + stringResource(R.string.sec)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
            ) {
                Text(
                    "rx: ${peerRxMB ?: 0.00} MB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Text(
                    "tx: ${peerTxMB ?: 0.00} MB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            if (endpoint != null) {
                AnimatedVisibility(visible = true) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
                    ) {
                        Text(
                            stringResource(R.string.endpoint).lowercase() + ": $endpoint",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
        }
    }
}
