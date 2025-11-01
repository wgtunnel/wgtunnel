package com.zaneschepke.wireguardautotunnel.ui.navigation.functions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.zaneschepke.wireguardautotunnel.ui.navigation.NavController

@Composable
fun <T : NavKey> rememberNavController(
    backStack: NavBackStack<NavKey>,
    isDisclosureShown: Boolean,
    onChange: (NavKey?) -> Unit = {},
): NavController {
    return remember(backStack, onChange, isDisclosureShown) {
        NavController(backStack, isDisclosureShown, onChange)
    }
}
