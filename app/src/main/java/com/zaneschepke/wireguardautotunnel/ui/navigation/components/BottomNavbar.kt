package com.zaneschepke.wireguardautotunnel.ui.navigation.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.ui.navigation.Tab
import com.zaneschepke.wireguardautotunnel.ui.theme.SilverTree

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BottomNavbar(isAutoTunnelActive: Boolean, currentTab: Tab, onTabSelected: (Tab) -> Unit) {
    FlexibleBottomAppBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        content = {
            val arrangement = BottomAppBarDefaults.FlexibleFixedHorizontalArrangement
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = arrangement) {
                Tab.entries.forEach { tab ->
                    val interactionSource = remember { MutableInteractionSource() }
                    val isSelected = currentTab == tab
                    val hasBadge = tab == Tab.AUTOTUNNEL && isAutoTunnelActive
                    IconButton(
                        onClick = { onTabSelected(tab) },
                        colors =
                            IconButtonDefaults.iconButtonColors(
                                containerColor = Color.Transparent,
                                contentColor =
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                disabledContainerColor = Color.Transparent,
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        interactionSource = interactionSource,
                    ) {
                        if (hasBadge) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        modifier =
                                            Modifier.offset(x = 8.dp, y = (-8).dp).size(6.dp),
                                        containerColor = SilverTree,
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = stringResource(tab.titleRes),
                                )
                            }
                        } else {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = stringResource(tab.titleRes),
                            )
                        }
                    }
                }
            }
        },
    )
}
