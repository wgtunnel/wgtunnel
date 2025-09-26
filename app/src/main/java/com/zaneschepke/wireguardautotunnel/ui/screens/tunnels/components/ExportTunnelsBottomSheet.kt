package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.components

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.enums.ConfigType
import com.zaneschepke.wireguardautotunnel.ui.common.functions.rememberFileExportLauncherForResult
import com.zaneschepke.wireguardautotunnel.ui.common.sheet.CustomBottomSheet
import com.zaneschepke.wireguardautotunnel.ui.common.sheet.SheetOption
import com.zaneschepke.wireguardautotunnel.util.FileUtils
import com.zaneschepke.wireguardautotunnel.util.extensions.hasSAFSupport
import java.time.Instant

@Composable
fun ExportTunnelsBottomSheet(
    onExport: (configType: ConfigType, uri: Uri?) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    var exportConfigType by remember { mutableStateOf(ConfigType.WG) }
    var shouldExport by remember { mutableStateOf(false) }

    val selectedTunnelsExportLauncher =
        rememberFileExportLauncherForResult(
            mimeType = FileUtils.ZIP_FILE_MIME_TYPE,
            onResult = { file ->
                if (file != null) {
                    onExport(exportConfigType, file)
                } else onDismiss()
            },
        )

    fun handleFileExport() {
        if (context.hasSAFSupport(FileUtils.ZIP_FILE_MIME_TYPE)) {
            val fileName =
                when (exportConfigType) {
                    ConfigType.AM -> "am_export_${Instant.now().epochSecond}.zip"
                    ConfigType.WG -> "wg_export_${Instant.now().epochSecond}.zip"
                }
            selectedTunnelsExportLauncher.launch(fileName)
        } else {
            onExport(exportConfigType, null)
        }
    }

    LaunchedEffect(shouldExport) {
        if (shouldExport) {
            handleFileExport()
            shouldExport = false
        }
    }

    CustomBottomSheet(
        listOf(
            SheetOption(
                Icons.Outlined.FolderZip,
                stringResource(R.string.export_tunnels_amnezia),
                onClick = {
                    exportConfigType = ConfigType.AM
                    shouldExport = true
                },
            ),
            SheetOption(
                Icons.Outlined.FolderZip,
                stringResource(R.string.export_tunnels_wireguard),
                onClick = {
                    exportConfigType = ConfigType.WG
                    shouldExport = true
                },
            ),
        )
    ) {
        onDismiss()
    }
}
