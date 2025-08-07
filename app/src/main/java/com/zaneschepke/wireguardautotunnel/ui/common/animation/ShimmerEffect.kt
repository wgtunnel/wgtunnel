package com.zaneschepke.wireguardautotunnel.ui.common.animation

import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun ShimmerEffect(modifier: Modifier = Modifier): Brush {
    val shimmerColors =
        listOf(
            Color.LightGray.copy(alpha = 0.9f),
            Color.LightGray.copy(alpha = 0.3f),
            Color.LightGray.copy(alpha = 0.9f),
        )

    val transition = rememberInfiniteTransition()
    val translateAnim by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec =
                infiniteRepeatable(animation = tween(durationMillis = 1200, easing = LinearEasing)),
        )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(0f, 0f),
        end = Offset(translateAnim, translateAnim),
    )
}
