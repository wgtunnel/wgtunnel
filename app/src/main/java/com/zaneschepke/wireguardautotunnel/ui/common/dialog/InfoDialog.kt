package com.zaneschepke.wireguardautotunnel.ui.common.dialog

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.zaneschepke.wireguardautotunnel.R

@Composable
fun InfoDialog(
    onAttest: () -> Unit,
    onDismiss: () -> Unit,
    title: @Composable () -> Unit,
    body: @Composable () -> Unit,
    confirmText: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy()) {
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            AlertDialog(
                onDismissRequest = { onDismiss() },
                confirmButton = { TextButton(onClick = { onAttest() }) { confirmText() } },
                dismissButton = {
                    TextButton(onClick = { onDismiss() }) {
                        Text(text = stringResource(R.string.cancel))
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { title() },
                text = { body() },
                properties = DialogProperties(usePlatformDefaultWidth = true),
            )
        }
    }
}
