package com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.disclosure.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PermScanWifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R

@Composable
fun LocationDisclosureHeader() {
    val icon = Icons.Rounded.PermScanWifi
    Icon(
        imageVector = icon,
        contentDescription = icon.name,
        modifier = Modifier.padding(24.dp).size(100.dp),
    )
    Text(
        text = stringResource(R.string.prominent_background_location_title),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
    )
    Text(
        text = stringResource(R.string.prominent_background_location_message),
        style = MaterialTheme.typography.bodyMedium,
    )
}
