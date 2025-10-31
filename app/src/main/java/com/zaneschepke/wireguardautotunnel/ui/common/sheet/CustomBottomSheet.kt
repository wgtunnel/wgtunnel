package com.zaneschepke.wireguardautotunnel.ui.common.sheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.LocalIsAndroidTV

@Composable
fun SheetOption(
    label: String,
    leadingIcon: ImageVector? = null,
    onClick: () -> Unit,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row {
            leadingIcon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp),
                )
            }
            Text(text = label, modifier = Modifier.padding(10.dp))
        }
        if (selected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = stringResource(R.string.selected),
                modifier = Modifier.padding(10.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomBottomSheet(options: List<SheetOption>, onDismiss: () -> Unit) {
    val isTv = LocalIsAndroidTV.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = isTv)
    ModalBottomSheet(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        options.forEachIndexed { index, option ->
            SheetOption(option.label, option.leadingIcon, option.onClick, option.selected)
            if (index != options.size - 1) HorizontalDivider()
        }
    }
}

data class SheetOption(
    val leadingIcon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val selected: Boolean = false,
)
