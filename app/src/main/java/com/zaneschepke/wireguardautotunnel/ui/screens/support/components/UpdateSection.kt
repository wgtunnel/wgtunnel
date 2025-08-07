package com.zaneschepke.wireguardautotunnel.ui.screens.support.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.BuildConfig
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItemLabel
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionLabelType
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton

@Composable
fun UpdateSection(onUpdateCheck: () -> Unit = {}) {
    SurfaceSelectionGroupButton(
        listOf(
            SelectionItem(
                leading = { Icon(Icons.Outlined.CloudDownload, contentDescription = null) },
                title = {
                    SelectionItemLabel(
                        stringResource(R.string.check_for_update),
                        SelectionLabelType.TITLE,
                    )
                },
                description = {
                    Column {
                        SelectionItemLabel(
                            stringResource(
                                R.string.version_template,
                                "v${BuildConfig.VERSION_NAME + 
                                    if(BuildConfig.DEBUG) "-debug" else "" }",
                            ),
                            SelectionLabelType.DESCRIPTION,
                        )
                        SelectionItemLabel(
                            stringResource(R.string.flavor_template, BuildConfig.FLAVOR),
                            SelectionLabelType.DESCRIPTION,
                        )
                    }
                },
                onClick = onUpdateCheck,
            )
        )
    )
}
