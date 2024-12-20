package com.zaneschepke.wireguardautotunnel.ui.common.button

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledHeight
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledWidth

@Composable
fun SelectionItemButton(
	leading: (@Composable () -> Unit)? = null,
	buttonText: String,
	trailing: (@Composable () -> Unit)? = null,
	onClick: () -> Unit,
	ripple: Boolean = true,
) {
	Card(
		modifier =
		Modifier
			.clip(RoundedCornerShape(8.dp))
			.clickable(
				indication = if (ripple) ripple() else null,
				interactionSource = remember { MutableInteractionSource() },
				onClick = { onClick() },
			)
			.height(56.dp.scaledHeight()),
		colors =
		CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.background,
		),
	) {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.Start,
			modifier = Modifier
				.fillMaxSize()
				.padding(end = 10.dp.scaledWidth()),
		) {
			leading?.let {
				it()
			}
			Text(
				buttonText,
				style = MaterialTheme.typography.labelMedium,
				color = MaterialTheme.colorScheme.onSurface,
				modifier = Modifier.fillMaxWidth(3 / 4f),
				maxLines = 2,
				overflow = TextOverflow.Ellipsis,
			)
			trailing?.let {
				it()
			}
		}
	}
}
