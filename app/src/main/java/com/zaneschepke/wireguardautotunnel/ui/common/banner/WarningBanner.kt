package com.zaneschepke.wireguardautotunnel.ui.common.banner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.theme.Straw

@Composable
fun WarningBanner(
    title: String,
    visible: Boolean,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    AnimatedVisibility(visible = visible, enter = expandVertically(), exit = shrinkVertically()) {
        Surface(
            color = MaterialTheme.colorScheme.secondary,
            modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(start = 2.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
                    modifier = Modifier.weight(4f, false).fillMaxWidth(),
                ) {
                    Icon(
                        Icons.Outlined.Warning,
                        stringResource(R.string.warning),
                        Modifier.size(18.dp),
                        tint = Straw,
                    )
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement =
                            Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(start = 6.dp),
                    ) {
                        Text(
                            title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                trailing?.let {
                    Box(
                        contentAlignment = Alignment.CenterEnd,
                        modifier = Modifier.padding(start = 16.dp),
                    ) {
                        it()
                    }
                }
            }
        }
    }
}
