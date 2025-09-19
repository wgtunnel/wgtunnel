package com.zaneschepke.wireguardautotunnel.ui.navigation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.navigation.BottomNavItem
import com.zaneschepke.wireguardautotunnel.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.ui.state.NavbarState
import com.zaneschepke.wireguardautotunnel.ui.theme.SilverTree

@Composable
fun NavHostController.getCurrentGraph(): State<Route?> {
    val navBackStackEntry by currentBackStackEntryAsState()

    return remember(navBackStackEntry) {
        derivedStateOf {
            val parentRouteString = navBackStackEntry?.destination?.parent?.route
            when (parentRouteString) {
                Route.TunnelsGraph::class.qualifiedName -> Route.TunnelsGraph
                Route.AutoTunnelGraph::class.qualifiedName -> Route.AutoTunnelGraph
                Route.SettingsGraph::class.qualifiedName -> Route.SettingsGraph
                Route.SupportGraph::class.qualifiedName -> Route.SupportGraph
                else -> null
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavbar(
    isAutoTunnelActive: Boolean,
    navbarState: NavbarState,
    navController: NavHostController,
) {

    val currentGraph by navController.getCurrentGraph()

    val items =
        listOf(
            BottomNavItem(
                name = stringResource(R.string.tunnels),
                icon = Icons.Rounded.Home,
                onClick = { navController.navigate(Route.TunnelsGraph) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                } },
                route = Route.TunnelsGraph,
            ),
            BottomNavItem(
                name = stringResource(R.string.auto_tunnel),
                icon = Icons.Rounded.Bolt,
                onClick = { navController.navigate(Route.AutoTunnelGraph) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                } },
                route = Route.AutoTunnelGraph,
                active = isAutoTunnelActive,
            ),
            BottomNavItem(
                name = stringResource(R.string.settings),
                icon = Icons.Rounded.Settings,
                onClick = { navController.navigate(Route.SettingsGraph) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                } },
                route = Route.SettingsGraph,
            ),
            BottomNavItem(
                name = stringResource(R.string.support),
                icon = Icons.Rounded.QuestionMark,
                onClick = { navController.navigate(Route.SupportGraph) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                } },
                route = Route.SupportGraph,
            ),
        )

    if (!navbarState.removeBottom) {
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            AnimatedVisibility(
                visible = navbarState.showBottomItems,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items.forEach { item ->
                        val interactionSource = remember { MutableInteractionSource() }
                        NavigationBarItem(
                            icon = {
                                if (item.active) {
                                    BadgedBox(
                                        badge = {
                                            Badge(
                                                modifier =
                                                    Modifier.offset(x = 8.dp, y = (-8).dp)
                                                        .size(6.dp),
                                                containerColor = SilverTree,
                                            )
                                        }
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.name,
                                        )
                                    }
                                } else {
                                    Icon(imageVector = item.icon, contentDescription = item.name)
                                }
                            },
                            onClick = item.onClick,
                            selected = currentGraph == item.route,
                            enabled = true,
                            label = null,
                            alwaysShowLabel = false,
                            colors =
                                NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onBackground,
                                    indicatorColor = Color.Transparent,
                                ),
                            interactionSource = interactionSource,
                        )
                    }
                }
            }
        }
    }
}
