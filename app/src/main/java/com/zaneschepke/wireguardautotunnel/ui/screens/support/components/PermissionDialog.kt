package com.zaneschepke.wireguardautotunnel.ui.screens.support.components

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.dialog.InfoDialog
import com.zaneschepke.wireguardautotunnel.util.extensions.requestInstallPackagesPermission

@Composable
fun PermissionDialog(context: Context, onDismiss: () -> Unit) {
    InfoDialog(
        onDismiss = onDismiss,
        onAttest = {
            context.requestInstallPackagesPermission()
            onDismiss()
        },
        title = stringResource(R.string.permission_required),
        body = { Text(stringResource(R.string.install_updated_permission)) },
        confirmText = stringResource(R.string.allow),
    )
}
