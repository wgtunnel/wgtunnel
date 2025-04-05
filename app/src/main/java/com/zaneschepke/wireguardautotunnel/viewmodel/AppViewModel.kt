package com.zaneschepke.wireguardautotunnel.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wireguard.android.backend.WgQuickBackend
import com.wireguard.android.util.RootShell
import com.zaneschepke.logcatter.LogReader
import com.zaneschepke.logcatter.model.LogMessage
import com.zaneschepke.networkmonitor.NetworkMonitor
import com.zaneschepke.networkmonitor.NetworkStatus
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.core.shortcut.ShortcutManager
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.data.model.GeneralState
import com.zaneschepke.wireguardautotunnel.di.AppShell
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.di.MainDispatcher
import com.zaneschepke.wireguardautotunnel.domain.entity.AppSettings
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendState
import com.zaneschepke.wireguardautotunnel.domain.enums.ConfigType
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.ui.state.AppViewState
import com.zaneschepke.wireguardautotunnel.ui.theme.Theme
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.FileReadException
import com.zaneschepke.wireguardautotunnel.util.FileUtils
import com.zaneschepke.wireguardautotunnel.util.InvalidFileExtensionException
import com.zaneschepke.wireguardautotunnel.util.LocaleUtil
import com.zaneschepke.wireguardautotunnel.util.StringValue
import com.zaneschepke.wireguardautotunnel.util.extensions.withFirstState
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.amnezia.awg.config.BadConfigException
import org.amnezia.awg.config.Config
import timber.log.Timber
import xyz.teamgravity.pin_lock_compose.PinManager
import java.net.URL
import java.time.Instant
import javax.inject.Inject
import javax.inject.Provider

@HiltViewModel
class AppViewModel
@Inject
constructor(
	val appDataRepository: AppDataRepository,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
	@MainDispatcher private val mainDispatcher: CoroutineDispatcher,
	@AppShell private val rootShell: Provider<RootShell>,
	private val tunnelManager: TunnelManager,
	private val serviceManager: ServiceManager,
	private val logReader: LogReader,
	private val fileUtils: FileUtils,
	private val shortcutManager: ShortcutManager,
	private val networkMonitor: NetworkMonitor,
) : ViewModel() {

	private val tunnelMutex = Mutex()
	private val settingsMutex = Mutex()
	private val tunControlMutex = Mutex()

	private val _screenCallback = MutableStateFlow<(() -> Unit)?>(null)

	private val _appViewState = MutableStateFlow(AppViewState())
	val appViewState = _appViewState.asStateFlow()

	private val _logs = MutableStateFlow<List<LogMessage>>(emptyList())
	val logs: StateFlow<List<LogMessage>> = _logs.asStateFlow()
	private val maxLogSize = 10_000

	val uiState =
		combine(
			appDataRepository.settings.flow,
			appDataRepository.tunnels.flow,
			appDataRepository.appState.flow,
			tunnelManager.activeTunnels,
			serviceManager.autoTunnelActive,
			networkMonitor.networkStatusFlow,
		) { array ->
			val settings = array[0] as AppSettings
			val tunnels = array[1] as List<TunnelConf>
			val generalState = array[2] as GeneralState
			val activeTunnels = array[3] as Map<TunnelConf, TunnelState>
			val autoTunnel = array[4] as Boolean
			val network = array[5] as NetworkStatus

			AppUiState(
				appSettings = settings,
				tunnels = tunnels,
				activeTunnels = activeTunnels,
				generalState = generalState,
				isAutoTunnelActive = autoTunnel,
				isAppLoaded = true,
				networkStatus = network,
			)
		}.stateIn(
			viewModelScope + ioDispatcher,
			SharingStarted.Companion.WhileSubscribed(Constants.SUBSCRIPTION_TIMEOUT),
			AppUiState(),
		)

	init {
		viewModelScope.launch(ioDispatcher) {
			uiState.withFirstState { state ->
				initPin(state.generalState.isPinLockEnabled)
				handleKillSwitchChange(state.appSettings)
				initServicesFromSavedState(state)
				if (state.generalState.isLocalLogsEnabled) startCollectingLogs()
			}
		}
	}

	fun handleEvent(event: AppEvent) = viewModelScope.launch(ioDispatcher) {
		uiState.withFirstState { state ->
			when (event) {
				AppEvent.ToggleLocalLogging -> handleToggleLocalLogging(state.generalState.isLocalLogsEnabled)
				is AppEvent.SetDebounceDelay -> handleSetDebounceDelay(state.appSettings, event.delay)
				is AppEvent.CopyTunnel -> handleCopyTunnel(event.tunnel, state.tunnels)
				is AppEvent.DeleteTunnel -> handleDeleteTunnel(event.tunnel, state)
				is AppEvent.ImportTunnelFromClipboard -> handleClipboardImport(event.text, state.tunnels)
				is AppEvent.ImportTunnelFromFile -> handleImportTunnelFromFile(event.data, state.tunnels)
				is AppEvent.ImportTunnelFromUrl -> handleImportTunnelFromUrl(event.url, state.tunnels)
				is AppEvent.ImportTunnelFromQrCode -> handleImportTunnelFromQr(event.qrCode, state.tunnels)
				AppEvent.SetBatteryOptimizeDisableShown -> setBatteryOptimizeDisableShown()
				is AppEvent.StartTunnel -> handleStartTunnel(event.tunnel, state.appSettings)
				is AppEvent.StopTunnel -> handleStopTunnel(event.tunnel)
				AppEvent.ToggleAutoTunnel -> handleToggleAutoTunnel(state)
				AppEvent.ToggleTunnelStatsExpanded -> handleToggleExpandTunnelStats(state.generalState.isTunnelStatsExpanded)
				AppEvent.ToggleAlwaysOn -> handleToggleAlwaysOnVPN(state.appSettings)
				AppEvent.TogglePinLock -> handlePinLockToggled(state.generalState.isPinLockEnabled)
				AppEvent.SetLocationDisclosureShown -> setLocationDisclosureShown()
				is AppEvent.SetLocale -> handleLocaleChange(event.localeTag)
				AppEvent.ToggleRestartAtBoot -> handleToggleRestartAtBoot(state.appSettings)
				AppEvent.ToggleVpnKillSwitch -> handleToggleVpnKillSwitch(state.appSettings)
				AppEvent.ToggleLanOnKillSwitch -> handleToggleLanOnKillSwitch(state.appSettings)
				AppEvent.ToggleAppShortcuts -> handleToggleAppShortcuts(state.appSettings)
				AppEvent.ToggleKernelMode -> handleToggleKernelMode(state.appSettings)
				is AppEvent.SetTheme -> handleThemeChange(event.theme)
				is AppEvent.ToggleIpv4Preferred -> handleToggleIpv4(event.tunnel)
				is AppEvent.TogglePrimaryTunnel -> handleTogglePrimaryTunnel(event.tunnel)
				is AppEvent.SetTunnelPingCooldown -> handlePingCoolDownChange(event.tunnel, event.pingCooldown)
				is AppEvent.SetTunnelPingInterval -> handlePingIntervalChange(event.tunnel, event.pingInterval)
				is AppEvent.AddTunnelRunSSID -> handleAddTunnelRunSSID(event.ssid, event.tunnel, state.tunnels)
				is AppEvent.DeleteTunnelRunSSID -> handleRemoveTunnelRunSSID(event.ssid, event.tunnel)
				is AppEvent.ToggleEthernetTunnel -> handleToggleEthernetTunnel(event.tunnel)
				is AppEvent.ToggleMobileDataTunnel -> handleToggleMobileDataTunnel(event.tunnel)
				AppEvent.ToggleAutoTunnelOnCellular -> handleToggleAutoTunnelOnCellular(state.appSettings)
				AppEvent.ToggleAutoTunnelOnWifi -> handleToggleAutoTunnelOnWifi(state.appSettings)
				is AppEvent.DeleteTrustedSSID -> handleDeleteTrustedSSID(event.ssid, state.appSettings)
				AppEvent.ToggleAutoTunnelWildcards -> handleToggleAutoTunnelWildcards(state.appSettings)
				AppEvent.ToggleRootShellWifi -> handleToggleRootShellWifi(state.appSettings)
				is AppEvent.SaveTrustedSSID -> handleSaveTrustedSSID(event.ssid, state.appSettings)
				AppEvent.ToggleAutoTunnelOnEthernet -> handleToggleTunnelOnEthernet(state.appSettings)
				AppEvent.ToggleStopKillSwitchOnTrusted -> handleToggleStopKillSwitchOnTrusted(state.appSettings)
				AppEvent.ToggleStopTunnelOnNoInternet -> handleToggleStopOnNoInternet(state.appSettings)
				is AppEvent.ExportTunnels -> handleExportTunnels(event.configType, state.tunnels)
				AppEvent.ExportLogs -> handleExportLogs()
				AppEvent.MessageShown -> handleErrorShown()
				is AppEvent.TogglePingTunnelEnabled -> handleTogglePingTunnel(event.tunnel)
				is AppEvent.SetTunnelPingIp -> handleTunnelPingIpChange(event.tunnelConf, event.ip)
				AppEvent.ToggleBottomSheet -> handleToggleBottomSheet()
				AppEvent.DeleteLogs -> handleDeleteLogs()
				is AppEvent.SetScreenAction -> _screenCallback.update { event.callback }
				AppEvent.InvokeScreenAction -> _screenCallback.value?.invoke()
				is AppEvent.SetSelectedTunnel -> _appViewState.update { it.copy(selectedTunnel = event.tunnel) }
				AppEvent.VpnPermissionRequested -> requestVpnPermission(false)
				is AppEvent.AppReadyCheck -> handleAppReadyCheck(event.tunnels)
				is AppEvent.ShowMessage -> handleShowMessage(event.message)
				is AppEvent.PopBackStack -> _appViewState.update { it.copy(popBackStack = event.pop) }
				is AppEvent.ClearTunnelError -> tunnelManager.clearError(event.tunnel)
			}
		}
	}

	private fun startCollectingLogs() {
		viewModelScope.launch {
			logReader.bufferedLogs
				.flowOn(ioDispatcher)
				.collect { logMessage ->
					_logs.update { currentList ->
						val newList = currentList.toMutableList()
						if (newList.size >= maxLogSize) {
							newList.removeAt(0)
						}
						newList.add(logMessage)
						newList
					}
				}
		}
	}

	private suspend fun handleAppReadyCheck(tunnels: List<TunnelConf>) {
		if (tunnels.size == appDataRepository.tunnels.count()) {
			_appViewState.update { it.copy(isAppReady = true) }
		}
	}

	private fun handleToggleBottomSheet() = _appViewState.update {
		it.copy(showBottomSheet = !it.showBottomSheet)
	}

	private suspend fun handleTunnelPingIpChange(tunnelConf: TunnelConf, ip: String) = saveTunnel(
		tunnelConf.copy(pingIp = ip),
	)

	private suspend fun handleTogglePingTunnel(tunnel: TunnelConf) = saveTunnel(
		tunnel.copy(isPingEnabled = !tunnel.isPingEnabled),
	)

	private suspend fun handleToggleLocalLogging(currentlyEnabled: Boolean) {
		val enable = !currentlyEnabled
		appDataRepository.appState.setLocalLogsEnabled(enable)
		withContext(mainDispatcher) {
			if (enable) {
				logReader.start()
				startCollectingLogs()
			} else {
				logReader.stop()
				_logs.update { emptyList() }
			}
		}
	}

	private suspend fun handleSetDebounceDelay(appSettings: AppSettings, delay: Int) = saveSettings(
		appSettings.copy(debounceDelaySeconds = delay),
	)

	private suspend fun handleCopyTunnel(tunnel: TunnelConf, existingTunnels: List<TunnelConf>) = saveTunnel(
		TunnelConf(
			tunName = tunnel.generateUniqueName(existingTunnels.map { it.tunName }),
			wgQuick = tunnel.wgQuick,
			amQuick = tunnel.amQuick,
		),
	)

	private suspend fun handleDeleteTunnel(tunnel: TunnelConf, state: AppUiState) {
		if (state.tunnels.size == 1 || tunnel.isPrimaryTunnel) {
			serviceManager.stopAutoTunnel()
		}
		appDataRepository.tunnels.delete(tunnel)
	}

	private fun requestVpnPermission(request: Boolean) = _appViewState.update {
		it.copy(requestVpnPermission = request)
	}

	private fun requestBatteryPermission(request: Boolean) = _appViewState.update {
		it.copy(requestBatteryPermission = request)
	}

	private suspend fun handleStartTunnel(tunnel: TunnelConf, appSettings: AppSettings) {
		tunControlMutex.withLock {
			if (!tunnelManager.hasVpnPermission() && !appSettings.isKernelEnabled) return@withLock requestVpnPermission(true)
			tunnelManager.startTunnel(tunnel)
		}
	}

	private suspend fun handleStopTunnel(tunnel: TunnelConf) {
		tunControlMutex.withLock {
			tunnelManager.stopTunnel(tunnel)
		}
	}

	private suspend fun handleToggleAutoTunnel(state: AppUiState) {
		tunControlMutex.withLock {
			if (!state.appSettings.isAutoTunnelEnabled && !tunnelManager.hasVpnPermission() &&
				!state.appSettings.isKernelEnabled
			) {
				return@withLock requestVpnPermission(
					true,
				)
			}
			if (!state.generalState.isBatteryOptimizationDisableShown) return@withLock requestBatteryPermission(true)
			serviceManager.toggleAutoTunnel()
		}
	}

	private suspend fun handleToggleExpandTunnelStats(currentlyEnabled: Boolean) {
		appDataRepository.appState.setTunnelStatsExpanded(!currentlyEnabled)
	}

	private fun handleErrorShown() {
		_appViewState.update { it.copy(errorMessage = null) }
	}

	private fun handleShowMessage(message: StringValue) {
		_appViewState.update { it.copy(errorMessage = message) }
	}

	private fun popBackStack() {
		_appViewState.update { it.copy(popBackStack = true) }
	}

	private suspend fun handleImportTunnelFromFile(uri: Uri, tunnels: List<TunnelConf>) {
		runCatching {
			val tunnelConfigs = fileUtils.buildTunnelsFromUri(uri)
			val existingNames = tunnels.map { it.tunName }.toMutableList()
			val uniqueTunnelConfigs = tunnelConfigs.map { config ->
				val uniqueName = config.generateUniqueName(existingNames)
				existingNames.add(uniqueName)
				config.copy(tunName = uniqueName)
			}
			appDataRepository.tunnels.saveAll(uniqueTunnelConfigs)
		}.onFailure {
			when (it) {
				is FileReadException, is BadConfigException -> handleShowMessage(StringValue.StringResource(R.string.error_file_format))
				is InvalidFileExtensionException -> handleShowMessage(StringValue.StringResource(R.string.error_file_extension))
				else -> handleShowMessage(StringValue.StringResource(R.string.unknown_error))
			}
			Timber.e(it)
		}
	}

	private suspend fun handleClipboardImport(config: String, tunnels: List<TunnelConf>) {
		runCatching {
			val amConfig = TunnelConf.configFromAmQuick(config)
			val tunnelConf = TunnelConf.tunnelConfigFromAmConfig(amConfig)
			saveTunnel(tunnelConf.copy(tunName = tunnelConf.generateUniqueName(tunnels.map { it.tunName })))
		}.onFailure {
			Timber.e(it)
			handleShowMessage(StringValue.StringResource(R.string.error_file_format))
		}
	}

	private suspend fun handleImportTunnelFromUrl(urlString: String, tunnels: List<TunnelConf>) {
		runCatching {
			val url = URL(urlString)
			val fileName = urlString.substringAfterLast("/")
			if (!fileName.endsWith(Constants.CONF_FILE_EXTENSION)) {
				throw InvalidFileExtensionException
			}
			url.openStream().use { stream ->
				val amConfig = Config.parse(stream)
				val tunnelConf = TunnelConf.tunnelConfigFromAmConfig(amConfig)
				saveTunnel(tunnelConf.copy(tunName = tunnelConf.generateUniqueName(tunnels.map { it.tunName })))
			}
		}.onFailure {
			Timber.e(it)
			val message = when (it) {
				is InvalidFileExtensionException -> StringValue.StringResource(R.string.error_file_extension)
				else -> StringValue.StringResource(R.string.error_download_failed)
			}
			handleShowMessage(message)
		}
	}

	private suspend fun handleImportTunnelFromQr(result: String, existingTunnels: List<TunnelConf>) {
		handleClipboardImport(result, existingTunnels)
		popBackStack()
	}

	private suspend fun setBatteryOptimizeDisableShown() {
		requestBatteryPermission(false)
		appDataRepository.appState.setBatteryOptimizationDisableShown(true)
	}

	private fun initServicesFromSavedState(state: AppUiState) = viewModelScope.launch(ioDispatcher) {
		tunControlMutex.withLock {
			if (state.appSettings.isAutoTunnelEnabled) serviceManager.startAutoTunnel()
			state.tunnels.filter { it.isActive }.forEach {
				tunnelManager.startTunnel(it)
			}
		}
	}

	private fun initPin(enabled: Boolean) {
		if (enabled) PinManager.initialize(WireGuardAutoTunnel.instance)
	}

	private suspend fun handlePinLockToggled(currentlyEnabled: Boolean) {
		if (currentlyEnabled) PinManager.clearPin()
		appDataRepository.appState.setPinLockEnabled(!currentlyEnabled)
	}

	private suspend fun setLocationDisclosureShown() {
		appDataRepository.appState.setLocationDisclosureShown(true)
	}

	private suspend fun handleToggleAlwaysOnVPN(appSettings: AppSettings) = saveSettings(
		appSettings.copy(
			isAlwaysOnVpnEnabled = !appSettings.isAlwaysOnVpnEnabled,
		),
	)

	private suspend fun handleLocaleChange(localeTag: String) {
		appDataRepository.appState.setLocale(localeTag)
		LocaleUtil.changeLocale(localeTag)
		_appViewState.update { it.copy(isConfigChanged = true) }
	}

	private suspend fun handleToggleRestartAtBoot(appSettings: AppSettings) = saveSettings(
		appSettings.copy(
			isRestoreOnBootEnabled = !appSettings.isRestoreOnBootEnabled,
		),
	)

	private suspend fun handleToggleVpnKillSwitch(appSettings: AppSettings) {
		val enabled = !appSettings.isVpnKillSwitchEnabled
		if (enabled && !tunnelManager.hasVpnPermission()) return requestVpnPermission(true)
		val updatedSettings = appSettings.copy(
			isVpnKillSwitchEnabled = enabled,
			isLanOnKillSwitchEnabled = if (enabled) appSettings.isLanOnKillSwitchEnabled else false,
		)
		saveSettings(updatedSettings)
		handleKillSwitchChange(updatedSettings)
	}

	private suspend fun handleToggleLanOnKillSwitch(appSettings: AppSettings) {
		val updatedSettings = appSettings.copy(
			isLanOnKillSwitchEnabled = !appSettings.isLanOnKillSwitchEnabled,
		)
		saveSettings(updatedSettings)
		handleKillSwitchChange(appSettings)
	}

	private fun handleKillSwitchChange(appSettings: AppSettings) {
		if (!appSettings.isVpnKillSwitchEnabled) return tunnelManager.setBackendState(BackendState.SERVICE_ACTIVE, emptyList())
		Timber.d("Starting kill switch")
		val allowedIps = if (appSettings.isLanOnKillSwitchEnabled) TunnelConf.LAN_BYPASS_ALLOWED_IPS else emptyList()
		tunnelManager.setBackendState(BackendState.KILL_SWITCH_ACTIVE, allowedIps)
	}

	private suspend fun handleToggleAppShortcuts(appSettings: AppSettings) {
		val enabled = !appSettings.isShortcutsEnabled
		if (enabled) shortcutManager.addShortcuts() else shortcutManager.removeShortcuts()
		saveSettings(
			appSettings.copy(
				isShortcutsEnabled = enabled,
			),
		)
	}

	private suspend fun handleTogglePrimaryTunnel(tunnelConf: TunnelConf) {
		tunnelMutex.withLock {
			appDataRepository.tunnels.updatePrimaryTunnel(
				when (tunnelConf.isPrimaryTunnel) {
					true -> null
					false -> tunnelConf
				},
			)
		}
	}

	private suspend fun handleToggleIpv4(tunnelConf: TunnelConf) = saveTunnel(
		tunnelConf.copy(
			isIpv4Preferred = !tunnelConf.isIpv4Preferred,
		),
	)

	private suspend fun handlePingIntervalChange(tunnelConf: TunnelConf, interval: String) = saveTunnel(
		tunnelConf.copy(pingInterval = if (interval.isBlank()) null else interval.toLong() * 1000),
	)

	private suspend fun handlePingCoolDownChange(tunnelConf: TunnelConf, cooldown: String) = saveTunnel(
		tunnelConf.copy(pingCooldown = if (cooldown.isBlank()) null else cooldown.toLong() * 1000),
	)

	private suspend fun handleThemeChange(theme: Theme) {
		appDataRepository.appState.setTheme(theme)
	}

	private suspend fun handleToggleKernelMode(appSettings: AppSettings) {
		val enabled = !appSettings.isKernelEnabled
		if (enabled && !isKernelSupported()) {
			handleShowMessage(StringValue.StringResource(R.string.kernel_not_supported))
			return
		}
		if (enabled && !requestRoot()) return
		// disable kill switch feature in kernel mode
		tunnelManager.setBackendState(BackendState.INACTIVE, emptyList())
		saveSettings(
			appSettings.copy(
				isKernelEnabled = enabled,
				isVpnKillSwitchEnabled = false,
				isLanOnKillSwitchEnabled = false,
			),
		)
	}

	private suspend fun handleRemoveTunnelRunSSID(ssid: String, tunnelConfig: TunnelConf) = saveTunnel(
		tunnelConfig.copy(
			tunnelNetworks = (tunnelConfig.tunnelNetworks - ssid).toMutableList(),
		),
	)

	private suspend fun handleAddTunnelRunSSID(ssid: String, tunnelConf: TunnelConf, existingTunnels: List<TunnelConf>) {
		if (ssid.isBlank()) return
		val trimmed = ssid.trim()
		if (existingTunnels.any { it.tunnelNetworks.contains(trimmed) }) return handleShowMessage(StringValue.StringResource(R.string.error_ssid_exists))
		saveTunnel(
			tunnelConf.copy(
				tunnelNetworks = (tunnelConf.tunnelNetworks + ssid).toMutableList(),
			),
		)
	}

	private suspend fun handleToggleMobileDataTunnel(tunnelConf: TunnelConf) {
		tunnelMutex.withLock {
			if (tunnelConf.isMobileDataTunnel) return appDataRepository.tunnels.updateMobileDataTunnel(null)
			appDataRepository.tunnels.updateMobileDataTunnel(tunnelConf)
		}
	}

	private suspend fun handleToggleEthernetTunnel(tunnelConf: TunnelConf) {
		tunnelMutex.withLock {
			if (tunnelConf.isEthernetTunnel) return appDataRepository.tunnels.updateEthernetTunnel(null)
			appDataRepository.tunnels.updateEthernetTunnel(tunnelConf)
		}
	}

	private suspend fun handleToggleAutoTunnelOnWifi(appSettings: AppSettings) = saveSettings(
		appSettings.copy(
			isTunnelOnWifiEnabled = !appSettings.isTunnelOnWifiEnabled,
		),
	)

	private suspend fun handleToggleAutoTunnelOnCellular(appSettings: AppSettings) = saveSettings(
		appSettings.copy(
			isTunnelOnMobileDataEnabled = !appSettings.isTunnelOnMobileDataEnabled,
		),
	)

	private suspend fun handleToggleAutoTunnelWildcards(appSettings: AppSettings) = saveSettings(
		appSettings.copy(
			isWildcardsEnabled = !appSettings.isWildcardsEnabled,
		),
	)

	private suspend fun handleDeleteTrustedSSID(ssid: String, appSettings: AppSettings) = saveSettings(
		appSettings.copy(
			trustedNetworkSSIDs = (appSettings.trustedNetworkSSIDs - ssid).toMutableList(),
		),
	)

	private suspend fun handleToggleRootShellWifi(appSettings: AppSettings) {
		if (requestRoot()) {
			saveSettings(
				appSettings.copy(isWifiNameByShellEnabled = !appSettings.isWifiNameByShellEnabled),
			)
		}
	}

	private suspend fun handleToggleTunnelOnEthernet(appSettings: AppSettings) = saveSettings(
		appSettings.copy(isTunnelOnEthernetEnabled = !appSettings.isTunnelOnEthernetEnabled),
	)

	private suspend fun handleSaveTrustedSSID(ssid: String, appSettings: AppSettings) {
		if (ssid.isEmpty()) return
		val trimmed = ssid.trim()
		if (appSettings.trustedNetworkSSIDs.contains(trimmed)) return handleShowMessage(StringValue.StringResource(R.string.error_ssid_exists))
		saveSettings(
			appSettings.copy(
				trustedNetworkSSIDs = (appSettings.trustedNetworkSSIDs + ssid).toMutableList(),
			),
		)
	}

	private suspend fun handleToggleStopOnNoInternet(appSettings: AppSettings) = saveSettings(
		appSettings.copy(isStopOnNoInternetEnabled = !appSettings.isStopOnNoInternetEnabled),
	)

	private suspend fun handleToggleStopKillSwitchOnTrusted(appSettings: AppSettings) = saveSettings(
		appSettings.copy(isDisableKillSwitchOnTrustedEnabled = !appSettings.isDisableKillSwitchOnTrustedEnabled),
	)

	private suspend fun isKernelSupported(): Boolean {
		return withContext(ioDispatcher) {
			WgQuickBackend.hasKernelSupport()
		}
	}

	private suspend fun saveSettings(appSettings: AppSettings) = withContext(ioDispatcher) {
		settingsMutex.withLock {
			appDataRepository.settings.save(appSettings)
		}
	}

	private suspend fun saveTunnel(tunnel: TunnelConf) = withContext(ioDispatcher) {
		tunnelMutex.withLock {
			appDataRepository.tunnels.save(tunnel)
		}
	}

	private suspend fun handleExportTunnels(configType: ConfigType, tunnels: List<TunnelConf>) {
		runCatching {
			val (files, shareFileName) = when (configType) {
				ConfigType.AMNEZIA -> {
					Pair(fileUtils.createAmFiles(tunnels), "am-export_${Instant.now().epochSecond}.zip")
				}
				ConfigType.WG -> {
					Pair(fileUtils.createWgFiles(tunnels), "wg-export_${Instant.now().epochSecond}.zip")
				}
			}
			val shareFile = fileUtils.createNewShareFile(shareFileName)
			fileUtils.zipAll(shareFile, files)
			fileUtils.shareFile(shareFile)
		}.onFailure {
			Timber.e(it)
			handleShowMessage(StringValue.StringResource(R.string.export_failed))
		}
	}

	private suspend fun handleExportLogs() {
		runCatching {
			val file = fileUtils.createNewShareFile("${Constants.BASE_LOG_FILE_NAME}-${Instant.now().epochSecond}.zip")
			logReader.zipLogFiles(file.absolutePath)
			fileUtils.shareFile(file)
		}.onFailure {
			Timber.e(it)
			handleShowMessage(StringValue.StringResource(R.string.export_failed))
		}
	}

	private suspend fun handleDeleteLogs() {
		withContext(mainDispatcher) {
			logReader.deleteAndClearLogs()
			_logs.update { emptyList() }
		}
	}

	private suspend fun requestRoot(): Boolean {
		return withContext(ioDispatcher) {
			try {
				rootShell.get().start()
				handleShowMessage(StringValue.StringResource(R.string.root_accepted))
				true
			} catch (e: Exception) {
				handleShowMessage(StringValue.StringResource(R.string.error_root_denied))
				false
			}
		}
	}
}
