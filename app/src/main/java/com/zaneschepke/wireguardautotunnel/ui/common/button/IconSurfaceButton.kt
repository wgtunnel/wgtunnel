package com.zaneschepke.wireguardautotunnel.ui.common.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItemLabel
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionLabelType

@Composable
fun IconSurfaceButton(
    title: String,
    onClick: () -> Unit,
    selected: Boolean,
    leading: (@Composable () -> Unit)? = null,
    description: String? = null,
) {
    val border: BorderStroke? =
        if (selected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    Card(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        shape = RoundedCornerShape(8.dp),
        border = border,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Box(modifier = Modifier.clickable { onClick() }.fillMaxWidth()) {
            Column(
                modifier =
                    Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
                        .padding(end = 16.dp)
                        .padding(start = 8.dp)
                        .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier.padding(vertical = if (description == null) 10.dp else 0.dp),
                    ) {
                        leading?.invoke()
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            SelectionItemLabel(title, SelectionLabelType.TITLE)
                            description?.let {
                                Text(
                                    description,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
