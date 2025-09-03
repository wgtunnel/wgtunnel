package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CopyAll
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.LocalSharedVm
import com.zaneschepke.wireguardautotunnel.ui.common.button.ActionIconButton
import com.zaneschepke.wireguardautotunnel.ui.common.dialog.InfoDialog
import com.zaneschepke.wireguardautotunnel.ui.common.functions.rememberClipboardHelper
import com.zaneschepke.wireguardautotunnel.ui.common.functions.rememberFileImportLauncherForResult
import com.zaneschepke.wireguardautotunnel.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.components.ExportTunnelsBottomSheet
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.components.TunnelImportSheet
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.components.TunnelList
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.components.UrlImportDialog
import com.zaneschepke.wireguardautotunnel.ui.sideeffect.LocalSideEffect
import com.zaneschepke.wireguardautotunnel.util.FileUtils
import com.zaneschepke.wireguardautotunnel.util.StringValue
import com.zaneschepke.wireguardautotunnel.viewmodel.TunnelsViewModel
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun TunnelsScreen(viewModel: TunnelsViewModel) {
    val sharedViewModel = LocalSharedVm.current
    val navController = LocalNavController.current
    val clipboard = rememberClipboardHelper()

    val tunnelsState by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    var showExportSheet by rememberSaveable { mutableStateOf(false) }
    var showImportSheet by rememberSaveable { mutableStateOf(false) }
    var showDeleteModal by rememberSaveable { mutableStateOf(false) }
    var showUrlDialog by rememberSaveable { mutableStateOf(false) }

    val disableDelete by
        remember(tunnelsState.activeTunnels, tunnelsState.selectedTunnels) {
            derivedStateOf {
                tunnelsState.activeTunnels.any { active ->
                    tunnelsState.selectedTunnels.any { it.id == active.key.id }
                }
            }
        }

    sharedViewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            LocalSideEffect.Sheet.ImportTunnels -> showImportSheet = true
            else -> Unit
        }
    }

    LaunchedEffect(tunnelsState.selectedTunnels) {
        when (val count = tunnelsState.selectedTunnels.count()) {
            0 -> sharedViewModel.updateTopNavActions(null)
            else -> {
                sharedViewModel.updateTopNavActions {
                    Row {
                        ActionIconButton(Icons.Rounded.SelectAll, R.string.select_all) {
                            viewModel.toggleSelectAllTunnels()
                        }
                        // due to permissions, and SAF issues on TV, not support less than Android
                        // 10
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            ActionIconButton(Icons.Rounded.Download, R.string.download) {
                                showExportSheet = true
                            }
                        }

                        if (count == 1) {
                            ActionIconButton(Icons.Rounded.CopyAll, R.string.copy) {
                                viewModel.copySelectedTunnel()
                            }
                        }

                        if (!disableDelete) {
                            ActionIconButton(Icons.Rounded.Delete, R.string.delete_tunnel) {
                                showDeleteModal = true
                            }
                        }
                    }
                }
            }
        }
    }

    if (!tunnelsState.stateInitialized) return

    val tunnelFileImportResultLauncher =
        rememberFileImportLauncherForResult(
            onNoFileExplorer = {
                sharedViewModel.showSnackMessage(
                    StringValue.StringResource(R.string.error_no_file_explorer)
                )
            },
            onData = { data -> viewModel.importFromUri(data) },
        )

    val scanLauncher =
        rememberLauncherForActivityResult(
            contract = ScanContract(),
            onResult = { result ->
                if (result != null && result.contents.isNotEmpty())
                    viewModel.importFromQr(result.contents)
            },
        )

    val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted
            ->
            if (!isGranted) {
                sharedViewModel.showSnackMessage(
                    StringValue.StringResource(R.string.camera_permission_required)
                )
                return@rememberLauncherForActivityResult
            }
            scanLauncher.launch(
                ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE).setBeepEnabled(false)
            )
        }

    if (showDeleteModal) {
        InfoDialog(
            onDismiss = { showDeleteModal = false },
            onAttest = {
                viewModel.deleteSelectedTunnels()
                showDeleteModal = false
            },
            title = { Text(text = stringResource(R.string.delete_tunnel)) },
            body = { Text(text = stringResource(R.string.delete_tunnel_message)) },
            confirmText = { Text(text = stringResource(R.string.yes)) },
        )
    }

    if (showExportSheet) {
        ExportTunnelsBottomSheet({ type, uri ->
            viewModel.exportSelectedTunnels(type, uri, tunnelsState.selectedTunnels)
        }) {
            showExportSheet = false
            viewModel.clearSelectedTunnels()
        }
    }

    if (showImportSheet) {
        TunnelImportSheet(
            onDismiss = { showImportSheet = false },
            onFileClick = {
                tunnelFileImportResultLauncher.launch(FileUtils.ALLOWED_TV_FILE_TYPES)
            },
            onQrClick = { requestPermissionLauncher.launch(android.Manifest.permission.CAMERA) },
            onClipboardClick = {
                clipboard.paste { result ->
                    if (result != null) viewModel.importFromClipboard(result)
                }
            },
            onManualImportClick = { navController.navigate(Route.Config(null)) },
            onUrlClick = { showUrlDialog = true },
        )
    }

    if (showUrlDialog) {
        UrlImportDialog(
            onDismiss = { showUrlDialog = false },
            onConfirm = { url ->
                viewModel.importFromUrl(url)
                showUrlDialog = false
            },
        )
    }

    TunnelList(
        tunnelsState,
        modifier = Modifier.fillMaxSize().padding(vertical = 24.dp).padding(horizontal = 12.dp),
        viewModel,
        sharedViewModel,
        navController,
    )
}

@Composable
fun TunnelActionBar() {

    Row {}
}
