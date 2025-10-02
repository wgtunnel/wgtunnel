package com.zaneschepke.wireguardautotunnel.ui.screens.support.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.LocalBackStack
import com.zaneschepke.wireguardautotunnel.ui.common.button.ForwardButton
import com.zaneschepke.wireguardautotunnel.ui.common.button.LaunchButton
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItemLabel
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionLabelType
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.util.extensions.openWebUrl

@Composable
fun GeneralSupportOptions() {
    val context = LocalContext.current
    val backStack = LocalBackStack.current
    SurfaceSelectionGroupButton(
        items =
            buildList {
                add(
                    SelectionItem(
                        leading = { Icon(Icons.Outlined.Book, contentDescription = null) },
                        title = {
                            SelectionItemLabel(
                                stringResource(R.string.docs_description),
                                SelectionLabelType.TITLE,
                            )
                        },
                        trailing = {
                            LaunchButton {
                                context.openWebUrl(context.getString(R.string.docs_url))
                            }
                        },
                        onClick = { context.openWebUrl(context.getString(R.string.docs_url)) },
                    )
                )
                add(
                    SelectionItem(
                        leading = { Icon(Icons.Outlined.Policy, contentDescription = null) },
                        title = {
                            SelectionItemLabel(
                                stringResource(R.string.privacy_policy),
                                SelectionLabelType.TITLE,
                            )
                        },
                        trailing = {
                            LaunchButton {
                                context.openWebUrl(context.getString(R.string.privacy_policy_url))
                            }
                        },
                        onClick = {
                            context.openWebUrl(context.getString(R.string.privacy_policy_url))
                        },
                    )
                )
                add(
                    SelectionItem(
                        leading = { Icon(Icons.Outlined.Balance, contentDescription = null) },
                        title = {
                            SelectionItemLabel(
                                stringResource(R.string.licenses),
                                SelectionLabelType.TITLE,
                            )
                        },
                        trailing = { ForwardButton { backStack.add(Route.License) } },
                        onClick = { backStack.add(Route.License) },
                    )
                )
            }
    )
}
