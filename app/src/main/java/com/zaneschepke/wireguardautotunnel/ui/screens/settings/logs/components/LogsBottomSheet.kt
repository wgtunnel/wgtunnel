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
import com.zaneschepke.wireguardautotunnel.ui.state.AppViewState
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsBottomSheet(viewModel: AppViewModel) {
    CustomBottomSheet(
        listOf(
            SheetOption(
                Icons.Outlined.FolderZip,
                stringResource(R.string.export_logs),
                onClick = {
                    viewModel.handleEvent(
                        AppEvent.SetBottomSheet(AppViewState.BottomSheet.NONE)
                    )
                    viewModel.handleEvent(AppEvent.ExportLogs)
                }
            ),
            SheetOption(
                Icons.Outlined.Delete,
                stringResource(R.string.delete_logs),
                onClick = {
                    viewModel.handleEvent(
                        AppEvent.SetBottomSheet(AppViewState.BottomSheet.NONE)
                    )
                    viewModel.handleEvent(AppEvent.DeleteLogs)
                }
            )
        )
    ) {
        viewModel.handleEvent(
            AppEvent.SetBottomSheet(AppViewState.BottomSheet.NONE)
        )
    }
}
