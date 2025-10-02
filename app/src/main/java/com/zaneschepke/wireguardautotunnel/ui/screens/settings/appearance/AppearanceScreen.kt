package com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.ui.common.SectionDivider
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance.components.DisplayThemeItem
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance.components.LanguageItem
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance.components.NotificationsItem

@Composable
fun AppearanceScreen() {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
    ) {
        SurfaceSelectionGroupButton(items = listOf(LanguageItem()))
        SectionDivider()
        SurfaceSelectionGroupButton(items = listOf(NotificationsItem()))
        SectionDivider()
        SurfaceSelectionGroupButton(items = listOf(DisplayThemeItem()))
    }
}
