package com.zaneschepke.wireguardautotunnel.ui.screens.support.donate.crypto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.common.functions.rememberClipboardHelper
import com.zaneschepke.wireguardautotunnel.ui.screens.support.donate.crypto.components.addressItem

@Composable
fun AddressesScreen() {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
        modifier =
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
    ) {
        val clipboard = rememberClipboardHelper()
        SurfaceSelectionGroupButton(
            Address.allAddresses.map { addressItem(it) { address -> clipboard.copy(address) } }
        )
    }
}
