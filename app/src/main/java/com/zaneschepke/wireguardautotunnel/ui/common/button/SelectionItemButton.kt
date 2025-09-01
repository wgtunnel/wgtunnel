package com.zaneschepke.wireguardautotunnel.ui.common.button

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItemLabel
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionLabelType

@Composable
fun SelectionItemButton(
    buttonText: String,
    description: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    ripple: Boolean = true,
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
                .height(IntrinsicSize.Min)
                .padding(horizontal = 12.dp)
                .padding(end = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                leading?.let { it() }
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    SelectionItemLabel(
                        buttonText,
                        SelectionLabelType.TITLE,
                        modifier = Modifier.weight(1f).padding(end = 24.dp),
                    )
                    description?.let {
                        SelectionItemLabel(
                            it,
                            SelectionLabelType.DESCRIPTION,
                            modifier = Modifier.weight(1f).padding(end = 24.dp),
                        )
                    }
                }
            }
            trailing?.let { it() }
        }
    }
}
