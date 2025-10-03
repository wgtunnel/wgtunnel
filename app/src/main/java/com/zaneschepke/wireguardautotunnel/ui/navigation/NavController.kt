package com.zaneschepke.wireguardautotunnel.ui.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

class NavController(
    private val backStack: NavBackStack<NavKey>,
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

    fun popUpTo(route: NavKey) {
        onChange(currentRoute)
        while (backStack.size > 1 && backStack.last() != route) {
            backStack.removeLast()
        }
        if (backStack.last() != route) {
            backStack.add(route)
        }
    }

    val currentRoute: NavKey?
        get() = backStack.lastOrNull()

    val canPop: Boolean
        get() = backStack.size > 1
}
