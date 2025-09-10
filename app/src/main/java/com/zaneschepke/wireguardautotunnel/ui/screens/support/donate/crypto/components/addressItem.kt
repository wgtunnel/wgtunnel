package com.zaneschepke.wireguardautotunnel.ui.screens.support.donate.crypto.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CopyAll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItemLabel
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionLabelType
import com.zaneschepke.wireguardautotunnel.ui.screens.support.donate.crypto.Address
import com.zaneschepke.wireguardautotunnel.ui.theme.iconSize

@Composable
fun addressItem(address: Address, onClick: (address: String) -> Unit): SelectionItem {
    val context = LocalContext.current
    val walletAddress = context.getString(address.address)
    var expand by rememberSaveable { mutableStateOf(false) }
    return SelectionItem(
        leading = {
            Image(
                painter = painterResource(id = address.icon),
                contentDescription = null,
                modifier = Modifier.size(iconSize),
            )
        },
        trailing = {
            IconButton(onClick = { onClick(walletAddress) }) {
                Icon(Icons.Outlined.CopyAll, contentDescription = null)
            }
        },
        title = { SelectionItemLabel(stringResource(address.name), SelectionLabelType.TITLE) },
        description = {
            Text(
                text = walletAddress,
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.outline
                    ),
                maxLines = if (expand) Int.MAX_VALUE else 1,
                overflow = if (expand) TextOverflow.Clip else TextOverflow.Ellipsis,
                modifier = Modifier.animateContentSize(),
            )
        },
        onClick = { expand = !expand },
    )
}
