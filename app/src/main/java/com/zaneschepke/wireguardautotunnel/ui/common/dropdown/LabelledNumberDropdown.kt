package com.zaneschepke.wireguardautotunnel.ui.common.dropdown

import androidx.compose.runtime.*
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton

@Composable
fun LabelledNumberDropdown(title: @Composable () -> Unit, description: (@Composable () -> Unit)? = null, leading: @Composable () -> Unit, onSelected: (Int?) -> Unit, options: List<Int?>, currentValue: Int?) {
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
                        onValueSelected = { num ->
                            onSelected(num)
                        },
                        isExpanded = isDropDownExpanded,
                        onDismiss = { isDropDownExpanded = false },
                    )
                },
            )
        )
    )
}