package com.zaneschepke.wireguardautotunnel.ui.screens.settings.logs.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.sheet.CustomBottomSheet
import com.zaneschepke.wireguardautotunnel.ui.common.sheet.SheetOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsBottomSheet(onExport: () -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    CustomBottomSheet(
        listOf(
            SheetOption(
                Icons.Outlined.FolderZip,
                stringResource(R.string.export_logs),
                onClick = onExport,
            ),
            SheetOption(
                Icons.Outlined.Delete,
                stringResource(R.string.delete_logs),
                onClick = onDelete,
            ),
        )
    ) {
        onDismiss()
    }
}
