package com.zaneschepke.wireguardautotunnel.viewmodel.event

import android.net.Uri
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.enums.ConfigType
import com.zaneschepke.wireguardautotunnel.ui.state.AppViewState
import com.zaneschepke.wireguardautotunnel.ui.theme.Theme
import com.zaneschepke.wireguardautotunnel.util.StringValue

sealed class AppEvent {
    data class SetScreenAction(val callback: () -> Unit) : AppEvent()

    data object InvokeScreenAction : AppEvent()

    data object ToggleLocalLogging : AppEvent()

    data object ToggleRestartAtBoot : AppEvent()

    data object ToggleVpnKillSwitch : AppEvent()

    data object ToggleLanOnKillSwitch : AppEvent()

    data object ToggleAppShortcuts : AppEvent()

    data object ToggleKernelMode : AppEvent()

    data object ToggleAlwaysOn : AppEvent()

    data object TogglePinLock : AppEvent()

    data class TogglePrimaryTunnel(val tunnel: TunnelConf) : AppEvent()

    data class ToggleIpv4Preferred(val tunnel: TunnelConf) : AppEvent()

    data class TogglePingTunnelEnabled(val tunnel: TunnelConf) : AppEvent()

    data class SetTunnelPingInterval(val tunnel: TunnelConf, val pingInterval: String) : AppEvent()

    data class SetTunnelPingCooldown(val tunnel: TunnelConf, val pingCooldown: String) : AppEvent()

    data class SetTunnelPingIp(val tunnelConf: TunnelConf, val ip: String) : AppEvent()

    data class AddTunnelRunSSID(val ssid: String, val tunnel: TunnelConf) : AppEvent()

    data class DeleteTunnelRunSSID(val ssid: String, val tunnel: TunnelConf) : AppEvent()

    data class ToggleEthernetTunnel(val tunnel: TunnelConf) : AppEvent()

    data class ToggleMobileDataTunnel(val tunnel: TunnelConf) : AppEvent()

    data class SetDebounceDelay(val delay: Int) : AppEvent()

    data object ToggleAutoTunnel : AppEvent()

    data class StartTunnel(val tunnel: TunnelConf) : AppEvent()

    data class StopTunnel(val tunnel: TunnelConf) : AppEvent()

    data object DeleteSelectedTunnels : AppEvent()

    data object CopySelectedTunnel : AppEvent()

    data class ImportTunnelFromFile(val data: Uri) : AppEvent()

    data class ImportTunnelFromClipboard(val text: String) : AppEvent()

    data class ImportTunnelFromUrl(val url: String) : AppEvent()

    data class ImportTunnelFromQrCode(val qrCode: String) : AppEvent()

    data class ToggleTunnelStatsExpanded(val tunnelId: Int) : AppEvent()

    data object SetBatteryOptimizeDisableShown : AppEvent()

    data object SetLocationDisclosureShown : AppEvent()

    data class SetLocale(val localeTag: String) : AppEvent()

    data class SetTheme(val theme: Theme) : AppEvent()

    data object ToggleAutoTunnelOnWifi : AppEvent()

    data object ToggleAutoTunnelOnCellular : AppEvent()

    data object ToggleAutoTunnelOnEthernet : AppEvent()

    data object ToggleStopKillSwitchOnTrusted : AppEvent()

    data object ToggleStopTunnelOnNoInternet : AppEvent()

    data object ToggleAutoTunnelWildcards : AppEvent()

    data object ToggleRootShellWifi : AppEvent()

    data class DeleteTrustedSSID(val ssid: String) : AppEvent()

    data class SaveTrustedSSID(val ssid: String) : AppEvent()

    data class ExportSelectedTunnels(val configType: ConfigType, val uri: Uri?) : AppEvent()

    data object ExportLogs : AppEvent()

    data object DeleteLogs : AppEvent()

    data object MessageShown : AppEvent()

    data class ShowMessage(val message: StringValue) : AppEvent()

    data class PopBackStack(val pop: Boolean) : AppEvent()

    data class SetBottomSheet(val showSheet: AppViewState.BottomSheet) : AppEvent()

    data class ToggleSelectedTunnel(val tunnel: TunnelConf) : AppEvent()

    data object ClearSelectedTunnels : AppEvent()

    data object VpnPermissionRequested : AppEvent()

    data class AppReadyCheck(val tunnels: List<TunnelConf>) : AppEvent()

    data object ToggleRemoteControl : AppEvent()

    data class SetShowModal(val modalType: AppViewState.ModalType) : AppEvent()

    data object ToggleSelectAllTunnels : AppEvent()
}
