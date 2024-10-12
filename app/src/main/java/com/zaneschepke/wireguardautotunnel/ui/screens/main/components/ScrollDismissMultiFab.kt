package com.zaneschepke.wireguardautotunnel.ui.screens.main.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp

@Composable
fun ScrollDismissFab(icon: @Composable () -> Unit, focusRequester: FocusRequester, isVisible: Boolean, onClick: () -> Unit) {
	AnimatedVisibility(
		visible = isVisible,
		enter = slideInVertically(initialOffsetY = { it * 2 }),
		exit = slideOutVertically(targetOffsetY = { it * 2 }),
		modifier =
		Modifier
			.focusRequester(focusRequester)
			.focusGroup(),
	) {
		FloatingActionButton(
			onClick = {
				onClick()
			},
			shape = RoundedCornerShape(16.dp),
			containerColor = MaterialTheme.colorScheme.primary,
		) {
			icon()
		}
	}
}
