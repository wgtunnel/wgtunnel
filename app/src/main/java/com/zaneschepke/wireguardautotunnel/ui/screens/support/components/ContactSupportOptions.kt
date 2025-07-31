package com.zaneschepke.wireguardautotunnel.ui.screens.support.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.zaneschepke.wireguardautotunnel.BuildConfig
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ForwardButton
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItemLabel
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionLabelType
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.theme.iconSize
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.launchSupportEmail
import com.zaneschepke.wireguardautotunnel.util.extensions.openWebUrl

@Composable
fun ContactSupportOptions(context: android.content.Context) {
    SurfaceSelectionGroupButton(
        items =
            buildList {
                addAll(
                    listOf(
                        SelectionItem(
                            leading = {
                                Icon(
                                    ImageVector.vectorResource(R.drawable.matrix),
                                    contentDescription = null,
                                    Modifier.size(iconSize),
                                )
                            },
                            title = {
                                SelectionItemLabel(
                                    stringResource(R.string.join_matrix),
                                    SelectionLabelType.TITLE,
                                )
                            },
                            trailing = {
                                ForwardButton {
                                    context.openWebUrl(context.getString(R.string.matrix_url))
                                }
                            },
                            onClick = { context.openWebUrl(context.getString(R.string.matrix_url)) },
                        ),
                        SelectionItem(
                            leading = {
                                Icon(
                                    ImageVector.vectorResource(R.drawable.telegram),
                                    contentDescription = null,
                                    Modifier.size(iconSize),
                                )
                            },
                            title = {
                                SelectionItemLabel(
                                    stringResource(R.string.join_telegram),
                                    SelectionLabelType.TITLE,
                                )
                            },
                            trailing = {
                                ForwardButton {
                                    context.openWebUrl(context.getString(R.string.telegram_url))
                                }
                            },
                            onClick = {
                                context.openWebUrl(context.getString(R.string.telegram_url))
                            },
                        ),
                        SelectionItem(
                            leading = {
                                Icon(
                                    ImageVector.vectorResource(R.drawable.github),
                                    contentDescription = null,
                                    Modifier.size(iconSize),
                                )
                            },
                            title = {
                                SelectionItemLabel(
                                    stringResource(R.string.open_issue),
                                    SelectionLabelType.TITLE,
                                )
                            },
                            trailing = {
                                ForwardButton {
                                    context.openWebUrl(context.getString(R.string.github_url))
                                }
                            },
                            onClick = { context.openWebUrl(context.getString(R.string.github_url)) },
                        ),
                        SelectionItem(
                            leading = { Icon(Icons.Outlined.Mail, contentDescription = null) },
                            title = {
                                SelectionItemLabel(
                                    stringResource(R.string.email_description),
                                    SelectionLabelType.TITLE,
                                )
                            },
                            trailing = { ForwardButton { context.launchSupportEmail() } },
                            onClick = { context.launchSupportEmail() },
                        ),
                    )
                )
                if (BuildConfig.FLAVOR != Constants.GOOGLE_PLAY_FLAVOR) {
                    add(
                        SelectionItem(
                            leading = { Icon(Icons.Outlined.Favorite, contentDescription = null) },
                            title = {
                                SelectionItemLabel(
                                    stringResource(R.string.donate),
                                    SelectionLabelType.TITLE,
                                )
                            },
                            trailing = {
                                ForwardButton {
                                    context.openWebUrl(context.getString(R.string.donate_url))
                                }
                            },
                            onClick = { context.openWebUrl(context.getString(R.string.donate_url)) },
                        )
                    )
                }
            }
    )
}
