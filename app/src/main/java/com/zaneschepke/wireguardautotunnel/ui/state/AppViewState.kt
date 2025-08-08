package com.zaneschepke.wireguardautotunnel.ui.state

import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.util.StringValue

data class AppViewState(
    val isConfigChanged: Boolean = false,
    val errorMessage: StringValue? = null,
    val popBackStack: Boolean = false,
    val isAppReady: Boolean = false,
    val bottomSheet: BottomSheet = BottomSheet.NONE,
    val selectedTunnels: List<TunnelConf> = emptyList(),
    val requestVpnPermission: Boolean = false,
    val requestBatteryPermission: Boolean = false,
    val showModal: ModalType = ModalType.NONE,
) {
    enum class ModalType {
        NONE,
        DELETE,
        INFO,
        QR,
    }

    enum class BottomSheet {
        BACKUP_AND_RESTORE,
        EXPORT_TUNNELS,
        IMPORT_TUNNELS,
        LOGS,
        NONE,
    }
}
