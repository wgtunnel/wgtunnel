package com.zaneschepke.wireguardautotunnel.ui.navigation.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.navigation.BottomNavItem
import com.zaneschepke.wireguardautotunnel.ui.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.navigation.isCurrentRoute
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.ui.theme.SilverTree
import com.zaneschepke.wireguardautotunnel.util.extensions.goFromRoot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavbar(appUiState: AppUiState) {
    val navController = LocalNavController.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val items =
        listOf(
            BottomNavItem(
                name = stringResource(R.string.tunnels),
                route = Route.Main,
                icon = Icons.Rounded.Home,
                onClick = { navController.goFromRoot(Route.Main) },
            ),
            BottomNavItem(
                name = stringResource(R.string.auto_tunnel),
                route = Route.AutoTunnel,
                icon = Icons.Rounded.Bolt,
                onClick = {
                    val route =
                        if (appUiState.appState.isLocationDisclosureShown) {
                            Route.AutoTunnel
                        } else Route.LocationDisclosure
                    navController.goFromRoot(route)
                },
                active = appUiState.isAutoTunnelActive,
            ),
            BottomNavItem(
                name = stringResource(R.string.settings),
                route = Route.Settings,
                icon = Icons.Rounded.Settings,
                onClick = { navController.goFromRoot(Route.Settings) },
            ),
            BottomNavItem(
                name = stringResource(R.string.support),
                route = Route.Support,
                icon = Icons.Rounded.QuestionMark,
                onClick = { navController.goFromRoot(Route.Support) },
            ),
        )

    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        items.forEach { item ->
            val isSelected = navBackStackEntry.isCurrentRoute(item.route::class)
            val interactionSource = remember { MutableInteractionSource() }

            NavigationBarItem(
                icon = {
                    if (item.active) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    modifier = Modifier.offset(x = 8.dp, y = (-8).dp).size(6.dp),
                                    containerColor = SilverTree,
                                )
                            }
                        ) {
                            Icon(imageVector = item.icon, contentDescription = item.name)
                        }
                    } else {
                        Icon(imageVector = item.icon, contentDescription = item.name)
                    }
                },
                onClick = item.onClick,
                selected = isSelected,
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
