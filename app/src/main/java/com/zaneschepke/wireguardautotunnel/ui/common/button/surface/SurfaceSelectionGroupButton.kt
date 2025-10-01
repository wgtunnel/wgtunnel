package com.zaneschepke.wireguardautotunnel.ui.common.button.surface

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R

@Composable
fun SurfaceSelectionGroupButton(items: List<SelectionItem>, modifier: Modifier = Modifier) {
    if (items.isEmpty()) return
    val context = LocalContext.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items.forEach { item ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .alpha(if (item.isEnabled) 1f else 0.6f)
                            .semantics {
                                if (!item.isEnabled) {
                                    stateDescription =
                                        item.disabledReason ?: context.getString(R.string.disabled)
                                }
                            }
                            .combinedClickable(
                                onClick = { item.onClick?.invoke() },
                                onLongClick = { item.onLongPress?.invoke() },
                                enabled = true,
                            ),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier.weight(4f, false)
                                    .fillMaxWidth()
                                    .padding(vertical = item.padding),
                        ) {
                            item.leading?.let {
                                Box(modifier = Modifier.alpha(if (item.isEnabled) 1f else 0.6f)) {
                                    it()
                                }
                            }
                            Column(
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement =
                                    Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .padding(start = if (item.leading != null) 16.dp else 0.dp)
                                        .weight(1f),
                            ) {
                                Box(modifier = Modifier.alpha(if (item.isEnabled) 1f else 0.6f)) {
                                    item.title()
                                }
                                item.description?.invoke()
                                if (!item.isEnabled && item.disabledReason != null) {
                                    Text(
                                        text = item.disabledReason,
                                        style = MaterialTheme.typography.bodySmall,
                                        color =
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    )
                                }
                            }
                        }
                        item.trailing?.let {
                            Box(
                                contentAlignment = Alignment.CenterEnd,
                                modifier =
                                    Modifier.padding(start = 16.dp)
                                        .alpha(if (item.isEnabled) 1f else 0.6f)
                                        .run {
                                            if (!item.isEnabled) {
                                                semantics {
                                                    stateDescription =
                                                        context.getString(R.string.disabled)
                                                }
                                            } else {
                                                this
                                            }
                                        },
                            ) {
                                it()
                            }
                        }
                    }
                }
            }
        }
    }
}
