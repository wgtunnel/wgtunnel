package com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.advanced.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.DropdownSelector
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun DebounceDelaySelector(currentDelay: Int, onEvent: (AppEvent) -> Unit) {
    var isDropDownExpanded by remember { mutableStateOf(false) }

    SurfaceSelectionGroupButton(
        listOf(
            SelectionItem(
                leading = { Icon(Icons.Outlined.PauseCircle, contentDescription = null) },
                title = {
                    Text(
                        stringResource(R.string.debounce_delay),
                        style =
                            MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                    )
                },
                onClick = { isDropDownExpanded = true },
                trailing = {
                    DropdownSelector(
                        currentValue = currentDelay,
                        options = (0..10).toList(),
                        onValueSelected = { num -> onEvent(AppEvent.SetDebounceDelay(num)) },
                        isExpanded = isDropDownExpanded,
                        onDismiss = { isDropDownExpanded = false },
                    )
                },
            )
        )
    )
}
