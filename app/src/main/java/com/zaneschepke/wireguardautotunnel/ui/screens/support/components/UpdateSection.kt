package com.zaneschepke.wireguardautotunnel.ui.screens.support.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.BuildConfig
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItemLabel
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionLabelType
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.common.functions.rememberClipboardHelper

@Composable
fun UpdateSection(onUpdateCheck: () -> Unit) {
    val clipboardManager = rememberClipboardHelper()
    val version = remember {
        "v${BuildConfig.VERSION_NAME +
            if(BuildConfig.DEBUG) "-debug" else "" }"
    }
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
                            stringResource(R.string.version_template, version),
                            SelectionLabelType.DESCRIPTION,
                        )
                        SelectionItemLabel(
                            stringResource(R.string.flavor_template, BuildConfig.FLAVOR),
                            SelectionLabelType.DESCRIPTION,
                        )
                    }
                },
                onClick = onUpdateCheck,
                onLongPress = { clipboardManager.copy(version) },
            )
        )
    )
}
