package com.zaneschepke.wireguardautotunnel.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R

@Composable
fun <T> DropdownSelector(
    currentValue: T,
    options: List<T>,
    onValueSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    isExpanded: Boolean = false,
    onDismiss: () -> Unit = {},
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (label != null) label()
        Text(text = currentValue.toString(), style = MaterialTheme.typography.bodyMedium)
        Icon(Icons.Default.ArrowDropDown, contentDescription = stringResource(R.string.dropdown))
    }
    DropdownMenu(
        modifier = modifier.height(250.dp),
        scrollState = rememberScrollState(),
        containerColor = MaterialTheme.colorScheme.surface,
        expanded = isExpanded,
        onDismissRequest = onDismiss,
    ) {
        options.forEach { option ->
            DropdownMenuItem(
                text = { Text(text = option.toString()) },
                onClick = {
                    onValueSelected(option)
                    onDismiss() // Close dropdown after selection
                },
            )
        }
    }
}
