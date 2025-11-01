package com.zaneschepke.wireguardautotunnel.ui.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

class NavController(
    private val backStack: NavBackStack<NavKey>,
    private val isDisclosureShown: Boolean,
    private val onChange: (previous: NavKey?) -> Unit = {},
) {
    fun push(route: NavKey) {
        onChange(currentRoute)
        backStack.add(route)
    }

    fun pop(): Boolean {
        if (currentRoute != null) {
            onChange(currentRoute)
            backStack.removeLastOrNull()
            return true
        }
        return false
    }

    fun popUpTo(route: NavKey, inclusive: Boolean = false) {
        onChange(currentRoute)

        val targetRoute =
            if (route is Route.AutoTunnel && !isDisclosureShown) Route.LocationDisclosure else route

        val index = backStack.indexOfLast { it == targetRoute }
        if (index != -1) {
            val popUpToIndex = if (inclusive) index else index + 1
            while (backStack.size > popUpToIndex) {
                backStack.removeLastOrNull()
            }
        } else {
            // Only add if it's not already the top
            if (backStack.lastOrNull() != targetRoute) {
                backStack.add(targetRoute)
            }
        }
    }

    val currentRoute: NavKey?
        get() = backStack.lastOrNull()

    val canPop: Boolean
        get() = backStack.size > 1
}
