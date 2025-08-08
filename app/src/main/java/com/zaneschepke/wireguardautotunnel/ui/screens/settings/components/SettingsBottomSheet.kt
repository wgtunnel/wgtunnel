package com.zaneschepke.wireguardautotunnel.ui.screens.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.MainActivity
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.state.AppViewState
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(viewModel: AppViewModel) {
    val context = LocalContext.current

    ModalBottomSheet(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = {
            viewModel.handleEvent(AppEvent.SetBottomSheet(AppViewState.BottomSheet.NONE))
        },
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .clickable {
                        viewModel.handleEvent(
                            AppEvent.SetBottomSheet(AppViewState.BottomSheet.NONE)
                        )
                        (context as? MainActivity)?.performBackup()
                    }
                    .padding(10.dp)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.database),
                contentDescription = null,
                modifier = Modifier.padding(10.dp),
            )
            Text(
                text = stringResource(R.string.backup_application),
                modifier = Modifier.padding(10.dp),
            )
        }
        HorizontalDivider()
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .clickable {
                        viewModel.handleEvent(
                            AppEvent.SetBottomSheet(AppViewState.BottomSheet.NONE)
                        )
                        (context as? MainActivity)?.performRestore()
                    }
                    .padding(10.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Restore,
                contentDescription = null,
                modifier = Modifier.padding(10.dp),
            )
            Text(
                text = stringResource(R.string.restore_application),
                modifier = Modifier.padding(10.dp),
            )
        }
    }
}
