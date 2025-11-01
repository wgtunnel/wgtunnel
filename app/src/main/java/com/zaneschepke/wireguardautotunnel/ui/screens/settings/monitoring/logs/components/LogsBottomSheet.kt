package com.zaneschepke.wireguardautotunnel.ui.screens.settings.monitoring.logs.components

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.BuildConfig
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.functions.rememberFileExportLauncherForResult
import com.zaneschepke.wireguardautotunnel.ui.common.sheet.CustomBottomSheet
import com.zaneschepke.wireguardautotunnel.ui.common.sheet.SheetOption
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.FileUtils
import com.zaneschepke.wireguardautotunnel.util.extensions.hasSAFSupport

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsBottomSheet(onExport: (file: Uri?) -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current

    val selectedTunnelsExportLauncher =
        rememberFileExportLauncherForResult(
            mimeType = FileUtils.ZIP_FILE_MIME_TYPE,
            onResult = { file ->
                if (file != null) {
                    onExport(file)
                } else onDismiss()
            },
        )

    fun handleFileExport() {
        if (context.hasSAFSupport(FileUtils.ZIP_FILE_MIME_TYPE)) {
            selectedTunnelsExportLauncher.launch(
                "${Constants.BASE_LOG_FILE_NAME}_${BuildConfig.VERSION_NAME}_${BuildConfig.FLAVOR}.zip"
            )
        } else {
            onExport(null)
        }
    }

    CustomBottomSheet(
        listOf(
            SheetOption(
                Icons.Outlined.FolderZip,
                stringResource(R.string.export_logs),
                onClick = { handleFileExport() },
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
