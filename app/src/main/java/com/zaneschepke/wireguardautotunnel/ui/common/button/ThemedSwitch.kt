package com.zaneschepke.wireguardautotunnel.ui.common.button

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zaneschepke.wireguardautotunnel.ui.theme.Disabled

@Composable
fun ThemedSwitch(
    checked: Boolean,
    onClick: (checked: Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Switch(
        checked,
        { onClick(it) },
        modifier = modifier,
        enabled = enabled,
        colors =
            SwitchDefaults.colors()
                .copy(
                    checkedThumbColor = MaterialTheme.colorScheme.surface,
                    checkedIconColor = MaterialTheme.colorScheme.surface,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surface,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedIconColor = MaterialTheme.colorScheme.outline,
                    disabledUncheckedBorderColor = Disabled,
                    disabledUncheckedThumbColor = Disabled,
                    disabledUncheckedIconColor = Disabled,
                ),
    )
}
