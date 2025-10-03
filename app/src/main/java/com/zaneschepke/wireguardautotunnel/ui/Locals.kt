package com.zaneschepke.wireguardautotunnel.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.zaneschepke.wireguardautotunnel.ui.navigation.NavController
import com.zaneschepke.wireguardautotunnel.viewmodel.SharedAppViewModel

val LocalIsAndroidTV = staticCompositionLocalOf { false }

val LocalSharedVm =
    staticCompositionLocalOf<SharedAppViewModel> { error("No shared viewmodel provided") }

val LocalNavController = staticCompositionLocalOf<NavController> { error("No backstack provided") }

typealias BackStack = NavBackStack<NavKey>
