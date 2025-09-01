package com.zaneschepke.wireguardautotunnel.ui.screens.settings.proxy.compoents

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.zaneschepke.wireguardautotunnel.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.ui.common.sheet.CustomBottomSheet
import com.zaneschepke.wireguardautotunnel.ui.common.sheet.SheetOption
import com.zaneschepke.wireguardautotunnel.util.extensions.asIcon
import com.zaneschepke.wireguardautotunnel.util.extensions.asTitleString

@Composable
fun AppModeBottomSheet(
    onAppModeChange: (AppMode) -> Unit,
    appMode: AppMode,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    CustomBottomSheet(
        enumValues<AppMode>().map {
            val icon = it.asIcon()
            SheetOption(
                icon,
                label = it.asTitleString(context),
                onClick = {
                    onDismiss()
                    onAppModeChange(it)
                },
                selected = appMode == it,
            )
        }
    ) {
        onDismiss()
    }
}
