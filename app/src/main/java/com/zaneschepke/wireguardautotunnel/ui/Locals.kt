package com.zaneschepke.wireguardautotunnel.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController
import com.zaneschepke.wireguardautotunnel.viewmodel.SharedAppViewModel

val LocalNavController =
    compositionLocalOf<NavHostController> { error("NavController was not provided") }

val LocalIsAndroidTV = staticCompositionLocalOf { false }

val LocalSharedVm = staticCompositionLocalOf<SharedAppViewModel> { error("No SharedVm") }
