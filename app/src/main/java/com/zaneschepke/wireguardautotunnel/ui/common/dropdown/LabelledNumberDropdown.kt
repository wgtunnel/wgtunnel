package com.zaneschepke.wireguardautotunnel.ui.common.dropdown

import androidx.compose.runtime.*
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton

@Composable
fun <T> LabelledDropdown(
    title: @Composable () -> Unit,
    description: (@Composable () -> Unit)? = null,
    leading: @Composable () -> Unit,
    onSelected: (T?) -> Unit,
    options: List<T?>,
    currentValue: T?,
    optionToString: @Composable (T?) -> String,
) {
    var isDropDownExpanded by remember { mutableStateOf(false) }

    SurfaceSelectionGroupButton(
        listOf(
            SelectionItem(
                leading = leading,
                title = title,
                description = description,
                onClick = { isDropDownExpanded = true },
                trailing = {
                    DropdownSelector(
                        currentValue = currentValue,
                        options = options,
                        onValueSelected = { selected -> onSelected(selected) },
                        isExpanded = isDropDownExpanded,
                        onDismiss = { isDropDownExpanded = false },
                        optionToString = optionToString,
                    )
                },
            )
        )
    )
}
