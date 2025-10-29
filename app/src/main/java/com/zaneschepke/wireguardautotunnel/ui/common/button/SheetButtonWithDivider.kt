package com.zaneschepke.wireguardautotunnel.ui.common.button

import android.R.attr.onClick
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R

@Composable
fun SheetButtonWithDivider(
    showDivider: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showDivider) {
            VerticalDivider(
                modifier = Modifier.fillMaxHeight().padding(horizontal = 8.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Box(modifier = Modifier.pointerInput(Unit) { detectTapGestures {} }) {
            IconButton(onClick = onClick, modifier) {
                Icon(
                    Icons.Outlined.ExpandMore,
                    contentDescription = stringResource(R.string.select),
                )
            }
        }
    }
}
