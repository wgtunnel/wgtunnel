package com.zaneschepke.wireguardautotunnel.ui.common.button

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

@Composable
fun SurfaceRow(
    title: AnnotatedString,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    description: @Composable (() -> Unit)? = null,
    expandedContent: @Composable (() -> Unit)? = null,
    onLongClick: () -> Unit = {},
    enabled: Boolean = true,
    selected: Boolean = false,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable ((Modifier) -> Unit)? = null,
) {
    val density = LocalDensity.current
    var leadingPadding by remember { mutableStateOf(0.dp) }
    val interactionSource = remember { MutableInteractionSource() }
    val mainFocusRequester = remember { FocusRequester() }
    val trailingFocusRequester = remember { FocusRequester() }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                //                .focusGroup()
                .indication(interactionSource, ripple())
                .background(
                    if (!selected) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .defaultMinSize(minHeight = 42.dp)
                .animateContentSize(),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Row(
                modifier =
                    Modifier.focusRequester(mainFocusRequester)
                        .focusProperties {
                            if (onClick != null) {
                                right = trailingFocusRequester
                            }
                        }
                        .run {
                            if (onClick != null) {
                                combinedClickable(
                                    onClick = onClick,
                                    onLongClick = onLongClick,
                                    enabled = enabled,
                                    interactionSource = interactionSource,
                                    indication = null,
                                )
                            } else {
                                this
                            }
                        }
                        .run {
                            if (onClick != null) {
                                focusable()
                            } else {
                                this
                            }
                        },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                if (leading != null) {
                    Row(
                        modifier =
                            Modifier.onSizeChanged {
                                leadingPadding = with(density) { it.width.toDp() }
                            }
                    ) {
                        leading()
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                }
                Column(
                    modifier = Modifier.padding(end = 16.dp).weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color =
                            if (enabled) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    )
                    if (description != null) {
                        description()
                    }
                }
                if (trailing != null) {
                    trailing(
                        Modifier.focusRequester(trailingFocusRequester).focusProperties {
                            if (onClick != null) {
                                left = mainFocusRequester
                            }
                        }
                    )
                }
            }
        }
        if (expandedContent != null) {
            Row(modifier = Modifier.fillMaxWidth().padding(start = leadingPadding, top = 4.dp)) {
                expandedContent()
            }
        }
    }
}

@Composable
fun SurfaceRow(
    title: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    description: @Composable (() -> Unit)? = null,
    expandedContent: @Composable (() -> Unit)? = null,
    onLongClick: () -> Unit = {},
    enabled: Boolean = true,
    selected: Boolean = false,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable ((Modifier) -> Unit)? = null,
) {
    SurfaceRow(
        title = AnnotatedString(title),
        onClick = onClick,
        description = description,
        expandedContent = expandedContent,
        onLongClick = onLongClick,
        enabled = enabled,
        selected = selected,
        leading = leading,
        trailing = trailing,
        modifier = modifier,
    )
}
