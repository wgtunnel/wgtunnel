package com.zaneschepke.wireguardautotunnel.ui.screens.settings.proxy.compoents

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.zaneschepke.wireguardautotunnel.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.domain.model.AppSettings
import com.zaneschepke.wireguardautotunnel.ui.common.sheet.CustomBottomSheet
import com.zaneschepke.wireguardautotunnel.ui.common.sheet.SheetOption
import com.zaneschepke.wireguardautotunnel.ui.state.AppViewState
import com.zaneschepke.wireguardautotunnel.util.extensions.asIcon
import com.zaneschepke.wireguardautotunnel.util.extensions.asTitleString
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun BackendModeBottomSheet(appSettings: AppSettings, viewModel: AppViewModel) {
    val context = LocalContext.current
    CustomBottomSheet(
        enumValues<AppMode>().map {
            val icon = it.asIcon()
            SheetOption(
                icon,
                label = it.asTitleString(context),
                onClick = {
                    viewModel.handleEvent(AppEvent.SetBottomSheet(AppViewState.BottomSheet.NONE))
                    viewModel.handleEvent(AppEvent.SetAppMode(it))
                },
                selected = appSettings.appMode == it,
            )
        }
    ) {
        viewModel.handleEvent(AppEvent.SetBottomSheet(AppViewState.BottomSheet.NONE))
    }
}
