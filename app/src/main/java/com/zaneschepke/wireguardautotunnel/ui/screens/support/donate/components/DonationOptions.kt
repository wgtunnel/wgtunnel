package com.zaneschepke.wireguardautotunnel.ui.screens.support.donate.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CurrencyBitcoin
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ForwardButton
import com.zaneschepke.wireguardautotunnel.ui.common.button.LaunchButton
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItemLabel
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionLabelType
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.theme.iconSize
import com.zaneschepke.wireguardautotunnel.util.extensions.openWebUrl

@Composable
fun DonationOptions(onAddressesClick: () -> Unit) {
    val context = LocalContext.current
    SurfaceSelectionGroupButton(
        listOf(
            SelectionItem(
                leading = {
                    Icon(
                        Icons.Outlined.CurrencyBitcoin,
                        contentDescription = null,
                        modifier = Modifier.size(iconSize),
                    )
                },
                title = {
                    SelectionItemLabel(stringResource(R.string.crypto), SelectionLabelType.TITLE)
                },
                trailing = { ForwardButton { onAddressesClick() } },
                onClick = onAddressesClick,
            ),
            SelectionItem(
                leading = {
                    Icon(
                        ImageVector.vectorResource(R.drawable.github),
                        contentDescription = null,
                        modifier = Modifier.size(iconSize),
                    )
                },
                title = {
                    SelectionItemLabel(
                        stringResource(R.string.github_sponsors),
                        SelectionLabelType.TITLE,
                    )
                },
                trailing = {
                    LaunchButton {
                        context.openWebUrl(context.getString(R.string.github_sponsors_url))
                    }
                },
                onClick = { context.openWebUrl(context.getString(R.string.github_sponsors_url)) },
            ),
            SelectionItem(
                leading = {
                    Icon(
                        ImageVector.vectorResource(R.drawable.liberapay),
                        contentDescription = null,
                        modifier = Modifier.size(iconSize),
                    )
                },
                title = {
                    SelectionItemLabel(stringResource(R.string.liberapay), SelectionLabelType.TITLE)
                },
                trailing = {
                    LaunchButton { context.openWebUrl(context.getString(R.string.liberapay_url)) }
                },
                onClick = { context.openWebUrl(context.getString(R.string.liberapay_url)) },
            ),
            SelectionItem(
                leading = {
                    Icon(
                        ImageVector.vectorResource(R.drawable.kofi),
                        contentDescription = null,
                        modifier = Modifier.size(iconSize),
                    )
                },
                title = {
                    SelectionItemLabel(stringResource(R.string.kofi), SelectionLabelType.TITLE)
                },
                trailing = {
                    LaunchButton { context.openWebUrl(context.getString(R.string.kofi_url)) }
                },
                onClick = { context.openWebUrl(context.getString(R.string.kofi_url)) },
            ),
        )
    )
}
