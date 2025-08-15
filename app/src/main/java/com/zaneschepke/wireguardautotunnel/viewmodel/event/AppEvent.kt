package com.zaneschepke.wireguardautotunnel.viewmodel.event

import android.net.Uri
import com.zaneschepke.networkmonitor.AndroidNetworkMonitor
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendMode
import com.zaneschepke.wireguardautotunnel.domain.enums.ConfigType
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
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

    data object ToggleAlwaysOn : AppEvent()

    data object TogglePinLock : AppEvent()

    data object TogglePingMonitoring : AppEvent()

    data class TogglePrimaryTunnel(val tunnel: TunnelConf) : AppEvent()

    data class ToggleIpv4Preferred(val tunnel: TunnelConf) : AppEvent()

    data class ToggleRestartOnPingFailure(val tunnel: TunnelConf) : AppEvent()

    data class SetTunnelPingTarget(val tunnelConf: TunnelConf, val host: String) : AppEvent()

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

    data class SaveTunnel(val tunnel: TunnelConf) : AppEvent()

    data class SaveMonitoringSettings(
        val pingInterval: Int,
        val tunnelPingAttempts: Int,
        val pingTimeout: Int?,
    ) : AppEvent()

    data class SetDetectionMethod(val detectionMethod: AndroidNetworkMonitor.WifiDetectionMethod) :
        AppEvent()

    data class SetBackendMode(val backendMode: BackendMode) : AppEvent()

    data object ToggleAutoTunnelOnWifi : AppEvent()

    data object ToggleAutoTunnelOnCellular : AppEvent()

    data object ToggleAutoTunnelOnEthernet : AppEvent()

    data object ToggleStopKillSwitchOnTrusted : AppEvent()

    data object ToggleStopTunnelOnNoInternet : AppEvent()

    data object ToggleAutoTunnelWildcards : AppEvent()

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

    data class SaveAllConfigs(val tunnels: List<TunnelConf>) : AppEvent()

    data object ToggleShowDetailedPingStats : AppEvent()

    data class SetPingAttempts(val count: Int) : AppEvent()

    data class SetPingTimeout(val timeout: Int?) : AppEvent()

    data class SetPingInterval(val interval: Int) : AppEvent()

    data object ToggleSocks5Proxy : AppEvent()

    data object ToggleHttpProxy : AppEvent()

    data class SetSocks5BindAddress(val address : String) : AppEvent()

    data class SetHttpProxyBindAddress(val address : String) : AppEvent()

    data class SetProxyCredentials(val username: String, val password : String) : AppEvent()

}
