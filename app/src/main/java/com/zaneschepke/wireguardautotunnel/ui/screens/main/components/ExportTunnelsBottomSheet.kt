package com.zaneschepke.wireguardautotunnel.ui.screens.main.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.enums.ConfigType
import com.zaneschepke.wireguardautotunnel.ui.common.functions.rememberFileExportLauncherForResult
import com.zaneschepke.wireguardautotunnel.ui.common.sheet.CustomBottomSheet
import com.zaneschepke.wireguardautotunnel.ui.common.sheet.SheetOption
import com.zaneschepke.wireguardautotunnel.ui.navigation.LocalIsAndroidTV
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.AuthorizationPromptWrapper
import com.zaneschepke.wireguardautotunnel.ui.state.AppViewState
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.hasSAFSupport
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportTunnelsBottomSheet(viewModel: AppViewModel) {
    val context = LocalContext.current
    val isTv = LocalIsAndroidTV.current

    var exportConfigType by remember { mutableStateOf(ConfigType.WG) }
    var showAuthPrompt by remember { mutableStateOf(false) }
    var isAuthorized by remember { mutableStateOf(false) }
    var shouldExport by remember { mutableStateOf(false) }

    val selectedTunnelsExportLauncher =
        rememberFileExportLauncherForResult(
            mimeType = Constants.ZIP_FILE_MIME_TYPE,
            onResult = { file ->
                if (file != null) {
                    viewModel.handleEvent(AppEvent.ExportSelectedTunnels(exportConfigType, file))
                } else {
                    viewModel.handleEvent(AppEvent.ClearSelectedTunnels)
                    viewModel.handleEvent(AppEvent.SetBottomSheet(AppViewState.BottomSheet.NONE))
                }
            },
        )

    fun handleFileExport() {
        if (context.hasSAFSupport(Constants.ZIP_FILE_MIME_TYPE)) {
            selectedTunnelsExportLauncher.launch(Constants.DEFAULT_EXPORT_FILE_NAME)
        } else {
            viewModel.handleEvent(AppEvent.ExportSelectedTunnels(exportConfigType, null))
        }
    }

    LaunchedEffect(shouldExport) {
        if (shouldExport) {
            handleFileExport()
            shouldExport = false
        }
    }

    if (showAuthPrompt) {
        AuthorizationPromptWrapper(
            onDismiss = { showAuthPrompt = false },
            onSuccess = {
                showAuthPrompt = false
                isAuthorized = true
                shouldExport = true
            },
            viewModel = viewModel,
        )
    }

    CustomBottomSheet(listOf(
        SheetOption(
            Icons.Outlined.FolderZip,
            stringResource(R.string.export_tunnels_amnezia),
            onClick = {
                exportConfigType = ConfigType.AM
                if (!isAuthorized && !isTv) {
                    showAuthPrompt = true
                } else {
                    shouldExport = true
                }
            },
        ),
        SheetOption(
            Icons.Outlined.FolderZip,
            stringResource(R.string.export_tunnels_wireguard),
            onClick = {
                exportConfigType = ConfigType.WG
                if (!isAuthorized && !isTv) {
                    showAuthPrompt = true
                } else {
                    shouldExport = true
                }
            },
        ),
    )) {
        viewModel.handleEvent(AppEvent.SetBottomSheet(AppViewState.BottomSheet.NONE))
    }
}