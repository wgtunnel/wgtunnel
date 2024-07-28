package com.zaneschepke.wireguardautotunnel.ui.common

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaneschepke.wireguardautotunnel.service.tunnel.statistics.TunnelStatistics
import com.zaneschepke.wireguardautotunnel.util.NumberUtils
import com.zaneschepke.wireguardautotunnel.util.toThreeDecimalPlaceString

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RowListItem(
	icon: @Composable () -> Unit,
	text: String,
	onHold: () -> Unit,
	onClick: () -> Unit,
	rowButton: @Composable () -> Unit,
	expanded: Boolean,
	statistics: TunnelStatistics?,
	focusRequester: FocusRequester,
) {
	Box(
		modifier =
		Modifier
			.focusRequester(focusRequester)
			.animateContentSize()
			.clip(RoundedCornerShape(30.dp))
			.combinedClickable(
				onClick = { onClick() },
				onLongClick = { onHold() },
			),
	) {
		Column {
			Row(
				modifier =
				Modifier
					.fillMaxWidth()
					.padding(horizontal = 15.dp, vertical = 5.dp),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.SpaceBetween,
			) {
				Row(
					verticalAlignment = Alignment.CenterVertically,
					modifier = Modifier.fillMaxWidth(13 / 20f),
				) {
					icon()
					Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
				}
				rowButton()
			}
			if (expanded) {
				statistics?.getPeers()?.forEach {
					Row(
						modifier =
						Modifier
							.fillMaxWidth()
							.padding(end = 10.dp, bottom = 10.dp, start = 10.dp),
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.SpaceEvenly,
					) {
						// TODO change these to string resources
						val handshakeEpoch = statistics.peerStats(it)!!.latestHandshakeEpochMillis
						val peerTx = statistics.peerStats(it)!!.txBytes
						val peerRx = statistics.peerStats(it)!!.rxBytes
						val peerId = it.toBase64().subSequence(0, 3).toString() + "***"
						val handshakeSec =
							NumberUtils.getSecondsBetweenTimestampAndNow(handshakeEpoch)
						val handshake =
							if (handshakeSec == null) "never" else "$handshakeSec secs ago"
						val peerTxMB = NumberUtils.bytesToMB(peerTx).toThreeDecimalPlaceString()
						val peerRxMB = NumberUtils.bytesToMB(peerRx).toThreeDecimalPlaceString()
						val fontSize = 9.sp
						Text("peer: $peerId", fontSize = fontSize)
						Text("handshake: $handshake", fontSize = fontSize)
						Text("tx: $peerTxMB MB", fontSize = fontSize)
						Text("rx: $peerRxMB MB", fontSize = fontSize)
					}
				}
			}
		}
	}
}
