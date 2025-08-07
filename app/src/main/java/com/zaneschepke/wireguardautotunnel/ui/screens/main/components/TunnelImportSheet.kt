package com.zaneschepke.wireguardautotunnel.ui.screens.main.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.navigation.LocalIsAndroidTV

// TODO refactor this component
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunnelImportSheet(
    onDismiss: () -> Unit,
    onFileClick: () -> Unit,
    onQrClick: () -> Unit,
    onManualImportClick: () -> Unit,
    onClipboardClick: () -> Unit,
    onUrlClick: () -> Unit,
) {
    val isTv = LocalIsAndroidTV.current

    val sheetState = rememberModalBottomSheetState()

    val context = LocalContext.current
    ModalBottomSheet(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = { onDismiss() },
        sheetState = sheetState,
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .clickable {
                        onDismiss()
                        onFileClick()
                    }
                    .padding(10.dp)
        ) {
            Icon(
                Icons.Filled.FileOpen,
                contentDescription = stringResource(id = R.string.open_file),
                modifier = Modifier.padding(10.dp),
            )
            Text(stringResource(id = R.string.add_tunnels_text), modifier = Modifier.padding(10.dp))
        }
        if (!isTv) {
            HorizontalDivider()
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onQrClick()
                        }
                        .padding(10.dp)
            ) {
                Icon(
                    Icons.Filled.QrCode,
                    contentDescription = stringResource(id = R.string.qr_scan),
                    modifier = Modifier.padding(10.dp),
                )
                Text(stringResource(id = R.string.add_from_qr), modifier = Modifier.padding(10.dp))
            }
            HorizontalDivider()
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onClipboardClick()
                        }
                        .padding(10.dp)
            ) {
                val icon = Icons.Filled.ContentPasteGo
                Icon(icon, contentDescription = icon.name, modifier = Modifier.padding(10.dp))
                Text(
                    stringResource(id = R.string.add_from_clipboard),
                    modifier = Modifier.padding(10.dp),
                )
            }
        }
        HorizontalDivider()
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .clickable {
                        onDismiss()
                        onUrlClick()
                    }
                    .padding(10.dp)
        ) {
            Icon(
                Icons.Filled.Link,
                contentDescription = stringResource(id = R.string.add_from_url),
                modifier = Modifier.padding(10.dp),
            )
            Text(stringResource(id = R.string.add_from_url), modifier = Modifier.padding(10.dp))
        }
        HorizontalDivider()
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .clickable {
                        onDismiss()
                        onManualImportClick()
                    }
                    .padding(10.dp)
        ) {
            Icon(
                Icons.Filled.Create,
                contentDescription = stringResource(id = R.string.create_import),
                modifier = Modifier.padding(10.dp),
            )
            Text(stringResource(id = R.string.create_import), modifier = Modifier.padding(10.dp))
        }
    }
}
