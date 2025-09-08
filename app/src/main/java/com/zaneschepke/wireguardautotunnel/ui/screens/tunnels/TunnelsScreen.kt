package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.LocalSharedVm
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

    val sharedState by sharedViewModel.container.stateFlow.collectAsStateWithLifecycle()
    val tunnelsState by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    var showExportSheet by rememberSaveable { mutableStateOf(false) }
    var showImportSheet by rememberSaveable { mutableStateOf(false) }
    var showDeleteModal by rememberSaveable { mutableStateOf(false) }
    var showUrlDialog by rememberSaveable { mutableStateOf(false) }

    sharedViewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            LocalSideEffect.Sheet.ImportTunnels -> showImportSheet = true
            LocalSideEffect.Modal.DeleteTunnels -> showDeleteModal = true
            LocalSideEffect.Sheet.ExportTunnels -> showExportSheet = true
            else -> Unit
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
                sharedViewModel.deleteSelectedTunnels()
                showDeleteModal = false
            },
            title = { Text(text = stringResource(R.string.delete_tunnel)) },
            body = { Text(text = stringResource(R.string.delete_tunnel_message)) },
            confirmText = { Text(text = stringResource(R.string.yes)) },
        )
    }

    if (showExportSheet) {
        ExportTunnelsBottomSheet({ type, uri ->
            sharedViewModel.exportSelectedTunnels(type, uri)
        }) {
            showExportSheet = false
            sharedViewModel.clearSelectedTunnels()
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
        sharedState,
        modifier = Modifier.fillMaxSize().padding(vertical = 24.dp).padding(horizontal = 12.dp),
        sharedViewModel,
        navController,
    )
}
