package com.zaneschepke.wireguardautotunnel.domain.model

data class AppState(
    val isLocationDisclosureShown: Boolean = false,
    val isBatteryOptimizationDisableShown: Boolean = false,
)
