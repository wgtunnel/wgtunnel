package com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.LocalSharedVm
import com.zaneschepke.wireguardautotunnel.ui.common.SectionDivider
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance.components.DisplayThemeItem
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance.components.LanguageItem
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance.components.NotificationsItem
import com.zaneschepke.wireguardautotunnel.ui.state.NavbarState

@Composable
fun AppearanceScreen() {
    val sharedViewModel = LocalSharedVm.current
    val navController = LocalNavController.current
    LaunchedEffect(Unit) {
        sharedViewModel.updateNavbarState(
            NavbarState(
                showTopItems = true,
                showBottomItems = true,
                topTitle = { Text(stringResource(R.string.appearance)) },
            )
        )
    }

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
        modifier = Modifier.fillMaxSize().padding(vertical = 24.dp).padding(horizontal = 12.dp),
    ) {
        SurfaceSelectionGroupButton(items = listOf(LanguageItem(navController)))
        SectionDivider()
        SurfaceSelectionGroupButton(items = listOf(NotificationsItem()))
        SectionDivider()
        SurfaceSelectionGroupButton(items = listOf(DisplayThemeItem(navController)))
    }
}
