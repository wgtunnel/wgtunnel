package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.config.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.ui.state.ConfigProxy
import com.zaneschepke.wireguardautotunnel.ui.state.PeerProxy
import com.zaneschepke.wireguardautotunnel.ui.theme.iconSize

@Composable
fun PeersSection(
    isGlobalConfig: Boolean,
    configProxy: ConfigProxy,
    onRemove: (index: Int) -> Unit,
    onToggleLan: (index: Int) -> Unit,
    onUpdatePeer: (PeerProxy, index: Int) -> Unit,
) {
    configProxy.peers.forEachIndexed { index, peer ->
        var isDropDownExpanded by remember { mutableStateOf(false) }

        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 16.dp).focusGroup(),
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    GroupLabel(stringResource(R.string.peer))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if(!isGlobalConfig) IconButton(
                            modifier = Modifier.size(iconSize),
                            onClick = { onRemove(index) },
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = stringResource(R.string.delete),
                            )
                        }
                        Column {
                            IconButton(
                                modifier = Modifier.size(iconSize),
                                onClick = { isDropDownExpanded = true },
                            ) {
                                Icon(
                                    Icons.Rounded.MoreVert,
                                    contentDescription = stringResource(R.string.quick_actions),
                                )
                            }
                            DropdownMenu(
                                expanded = isDropDownExpanded,
                                onDismissRequest = { isDropDownExpanded = false },
                                modifier =
                                    Modifier.shadow(12.dp)
                                        .background(MaterialTheme.colorScheme.surface),
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (peer.isLanExcluded())
                                                stringResource(R.string.include_lan)
                                            else stringResource(R.string.exclude_lan)
                                        )
                                    },
                                    onClick = {
                                        onToggleLan(index)
                                        isDropDownExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
                PeerFields(isGlobalConfig, peer = peer, onPeerChange = { onUpdatePeer(it, index) })
            }
        }
    }
}
