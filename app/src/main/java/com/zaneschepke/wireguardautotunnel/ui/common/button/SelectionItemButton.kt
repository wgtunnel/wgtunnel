package com.zaneschepke.wireguardautotunnel.ui.common.button

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun SelectionItemButton(
    leading: (@Composable () -> Unit)? = null,
    buttonText: String,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
    ripple: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    indication = if (ripple) ripple() else null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = { onClick() },
                )
                .height(56.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        ) {
            leading?.let { it() }
            Text(
                buttonText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(3 / 4f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            trailing?.let { it() }
        }
    }
}
