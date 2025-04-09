package com.zaneschepke.wireguardautotunnel.ui.common.button

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp

@Composable
fun ScaledSwitch(
    checked: Boolean,
    onClick: (checked: Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Switch(
        checked,
        { onClick(it) },
        modifier.scale((52.dp / 52.dp)),
        enabled = enabled,
        colors =
            SwitchDefaults.colors()
                .copy(
                    checkedThumbColor = MaterialTheme.colorScheme.background,
                    checkedIconColor = MaterialTheme.colorScheme.background,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surface,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedIconColor = MaterialTheme.colorScheme.outline,
                ),
    )
}
