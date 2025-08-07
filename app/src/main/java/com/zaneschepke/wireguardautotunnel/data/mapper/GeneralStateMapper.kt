package com.zaneschepke.wireguardautotunnel.data.mapper

import com.zaneschepke.wireguardautotunnel.data.entity.GeneralState
import com.zaneschepke.wireguardautotunnel.domain.model.AppState

object GeneralStateMapper {
    fun toAppState(generalState: GeneralState): AppState =
        with(generalState) {
            AppState(
                isLocationDisclosureShown,
                isBatteryOptimizationDisableShown,
                isPinLockEnabled,
                expandedTunnelIds,
                isLocalLogsEnabled,
                isRemoteControlEnabled,
                showDetailedPingStats,
                remoteKey,
                locale,
                theme,
            )
        }

    fun toGeneralState(appState: AppState): GeneralState {
        return with(appState) {
            GeneralState(
                isLocationDisclosureShown,
                isBatteryOptimizationDisableShown,
                isPinLockEnabled,
                expandedTunnelIds,
                isLocalLogsEnabled,
                isRemoteControlEnabled,
                showDetailedPingStats,
                remoteKey,
                locale,
                theme,
            )
        }
    }
}
