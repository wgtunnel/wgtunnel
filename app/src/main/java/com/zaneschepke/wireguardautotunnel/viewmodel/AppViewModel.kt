package com.zaneschepke.wireguardautotunnel.viewmodel

import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wireguard.android.backend.WgQuickBackend
import com.wireguard.android.util.RootShell
import com.zaneschepke.logcatter.LogReader
import com.zaneschepke.logcatter.model.LogMessage
import com.zaneschepke.networkmonitor.AndroidNetworkMonitor
import com.zaneschepke.networkmonitor.NetworkMonitor
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.core.shortcut.ShortcutManager
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.di.AppShell
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.di.MainDispatcher
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendStatus
import com.zaneschepke.wireguardautotunnel.domain.enums.ConfigType
import com.zaneschepke.wireguardautotunnel.domain.events.BackendError
import com.zaneschepke.wireguardautotunnel.domain.model.AppSettings
import com.zaneschepke.wireguardautotunnel.domain.model.AppState
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.ui.state.AppViewState
import com.zaneschepke.wireguardautotunnel.ui.theme.Theme
import com.zaneschepke.wireguardautotunnel.util.*
import com.zaneschepke.wireguardautotunnel.util.extensions.addAllUnique
import com.zaneschepke.wireguardautotunnel.util.extensions.withFirstState
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent
import com.zaneschepke.wireguardautotunnel.viewmodel.event.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.net.URL
import java.time.Instant
import java.util.*
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.amnezia.awg.config.BadConfigException
import org.amnezia.awg.config.Config
import rikka.shizuku.Shizuku
import timber.log.Timber
import xyz.teamgravity.pin_lock_compose.PinManager

@HiltViewModel
class AppViewModel
@Inject
constructor(
    val appDataRepository: AppDataRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
    @AppShell private val rootShell: Provider<RootShell>,
    val tunnelManager: TunnelManager,
    private val serviceManager: ServiceManager,
    private val logReader: LogReader,
    private val fileUtils: FileUtils,
    private val shortcutManager: ShortcutManager,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private var logsJob: Job? = null

    private val _eventChannel = Channel<AppEvent>(Channel.BUFFERED)

    private val tunnelMutex = Mutex()
    private val settingsMutex = Mutex()
    private val tunControlMutex = Mutex()
    private val loggerMutex = Mutex()

    private val _screenCallback = MutableStateFlow<(() -> Unit)?>(null)

    private val _appViewState = MutableStateFlow(AppViewState())
    val appViewState: StateFlow<AppViewState> = _appViewState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    private val _logs = MutableStateFlow<List<LogMessage>>(emptyList())
    val logs: StateFlow<List<LogMessage>> = _logs.asStateFlow()
    private val maxLogSize = Constants.MAX_LOG_SIZE

    val uiState: StateFlow<AppUiState> =
        combine(
                combine(
                    appDataRepository.settings.flow,
                    appDataRepository.tunnels.flow,
                    appDataRepository.appState.flow,
                ) { settings, tunnels, appState ->
                    Triple(settings, tunnels, appState)
                },
                combine(
                    tunnelManager.activeTunnels,
                    serviceManager.autoTunnelService.map { it != null },
                ) { activeTunnels, autoTunnel ->
                    Pair(activeTunnels, autoTunnel)
                },
                networkMonitor.connectivityStateFlow,
            ) { repoTriple, managerPair, network ->
                val (settings, tunnels, appState) = repoTriple
                val (activeTunnels, autoTunnel) = managerPair

                AppUiState(
                    appSettings = settings,
                    tunnels = tunnels,
                    activeTunnels = activeTunnels,
                    appState = appState,
                    isAutoTunnelActive = autoTunnel,
                    isAppLoaded = true,
                    connectivityState = network,
                )
            }
            .stateIn(
                viewModelScope + ioDispatcher,
                SharingStarted.WhileSubscribed(Constants.SUBSCRIPTION_TIMEOUT),
                AppUiState(),
            )

    init {
        viewModelScope.launch(ioDispatcher) {
            uiState.withFirstState { state ->
                initPin(state.appState.isPinLockEnabled)
                handleKillSwitchChange(state.appSettings)
                initServicesFromSavedState(state)
                if (state.appState.isLocalLogsEnabled) logsJob = startCollectingLogs()
                handleTunnelMessages()
            }
            for (event in _eventChannel) {
                val state = uiState.value
                when (event) {
                    AppEvent.ToggleLocalLogging ->
                        handleToggleLocalLogging(state.appState.isLocalLogsEnabled)

                    is AppEvent.SetDebounceDelay ->
                        handleSetDebounceDelay(state.appSettings, event.delay)

                    is AppEvent.CopySelectedTunnel -> handleCopySelectedTunnel(state.tunnels)
                    is AppEvent.DeleteSelectedTunnels -> handleDeleteSelectedTunnels()
                    is AppEvent.ImportTunnelFromClipboard ->
                        handleClipboardImport(event.text, state.tunnels)

                    is AppEvent.ImportTunnelFromFile -> handleImportTunnelFromFile(event.data)

                    is AppEvent.ImportTunnelFromUrl ->
                        handleImportTunnelFromUrl(event.url, state.tunnels)

                    is AppEvent.ImportTunnelFromQrCode ->
                        handleImportTunnelFromQr(event.qrCode, state.tunnels)

                    AppEvent.SetBatteryOptimizeDisableShown -> setBatteryOptimizeDisableShown()
                    is AppEvent.StartTunnel -> handleStartTunnel(event.tunnel, state.appSettings)
                    is AppEvent.StopTunnel -> handleStopTunnel(event.tunnel)
                    AppEvent.ToggleAutoTunnel -> handleToggleAutoTunnel(state)
                    is AppEvent.ToggleTunnelStatsExpanded ->
                        handleToggleTunnelStats(event.tunnelId, state.appState)

                    AppEvent.ToggleAlwaysOn -> handleToggleAlwaysOnVPN(state.appSettings)
                    AppEvent.TogglePinLock -> handlePinLockToggled(state.appState.isPinLockEnabled)
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
                    is AppEvent.AddTunnelRunSSID ->
                        handleAddTunnelRunSSID(event.ssid, event.tunnel, state.tunnels)

                    is AppEvent.DeleteTunnelRunSSID ->
                        handleRemoveTunnelRunSSID(event.ssid, event.tunnel)

                    is AppEvent.ToggleEthernetTunnel -> handleToggleEthernetTunnel(event.tunnel)
                    is AppEvent.ToggleMobileDataTunnel -> handleToggleMobileDataTunnel(event.tunnel)
                    AppEvent.ToggleAutoTunnelOnCellular ->
                        handleToggleAutoTunnelOnCellular(state.appSettings)

                    AppEvent.ToggleAutoTunnelOnWifi ->
                        handleToggleAutoTunnelOnWifi(state.appSettings)

                    is AppEvent.DeleteTrustedSSID ->
                        handleDeleteTrustedSSID(event.ssid, state.appSettings)

                    AppEvent.ToggleAutoTunnelWildcards ->
                        handleToggleAutoTunnelWildcards(state.appSettings)

                    is AppEvent.SaveTrustedSSID ->
                        handleSaveTrustedSSID(event.ssid, state.appSettings)

                    AppEvent.ToggleAutoTunnelOnEthernet ->
                        handleToggleTunnelOnEthernet(state.appSettings)

                    AppEvent.ToggleStopKillSwitchOnTrusted ->
                        handleToggleStopKillSwitchOnTrusted(state.appSettings)

                    AppEvent.ToggleStopTunnelOnNoInternet ->
                        handleToggleStopOnNoInternet(state.appSettings)

                    is AppEvent.ExportSelectedTunnels ->
                        handleExportSelectedTunnels(event.configType, event.uri)

                    AppEvent.ExportLogs -> handleExportLogs()
                    AppEvent.MessageShown -> handleErrorShown()
                    is AppEvent.ToggleRestartOnPingFailure -> handleTogglePingTunnel(event.tunnel)
                    is AppEvent.SetTunnelPingTarget ->
                        handleTunnelPingTargetChange(event.tunnelConf, event.host)

                    is AppEvent.SetBottomSheet -> handleSetBottomSheet(event.showSheet)
                    AppEvent.DeleteLogs -> handleDeleteLogs()
                    is AppEvent.SetScreenAction -> _screenCallback.update { event.callback }
                    AppEvent.InvokeScreenAction -> _screenCallback.value?.invoke()
                    is AppEvent.ToggleSelectedTunnel -> handleToggleSelectedTunnel(event.tunnel)
                    is AppEvent.ToggleSelectAllTunnels ->
                        handleToggleSelectAllTunnels(state.tunnels)

                    AppEvent.VpnPermissionRequested -> requestVpnPermission(false)
                    is AppEvent.AppReadyCheck -> handleAppReadyCheck(event.tunnels)
                    is AppEvent.ShowMessage -> handleShowMessage(event.message)
                    is AppEvent.PopBackStack ->
                        _appViewState.update { it.copy(popBackStack = event.pop) }

                    AppEvent.ToggleRemoteControl -> handleToggleRemoteControl(state.appState)
                    AppEvent.ClearSelectedTunnels -> clearSelectedTunnels()
                    is AppEvent.SetShowModal ->
                        _appViewState.update { it.copy(showModal = event.modalType) }

                    is AppEvent.SetDetectionMethod ->
                        handleSetDetectionMethod(event.detectionMethod, state.appSettings)

                    is AppEvent.SaveAllConfigs -> saveAllTunnels(event.tunnels)
                    AppEvent.ToggleShowDetailedPingStats ->
                        handleToggleShowDetailedPingStats(state.appState)

                    is AppEvent.SaveMonitoringSettings ->
                        handleMonitoringSaveChanges(
                            state.appSettings,
                            event.pingInterval,
                            event.tunnelPingAttempts,
                            event.pingTimeout,
                        )

                    AppEvent.TogglePingMonitoring -> handleTogglePingMonitoring(state.appSettings)
                    is AppEvent.SetPingAttempts ->
                        saveSettings(state.appSettings.copy(tunnelPingAttempts = event.count))

                    is AppEvent.SetPingInterval ->
                        saveSettings(
                            state.appSettings.copy(tunnelPingIntervalSeconds = event.interval)
                        )

                    is AppEvent.SetPingTimeout ->
                        saveSettings(
                            state.appSettings.copy(tunnelPingTimeoutSeconds = event.timeout)
                        )
                    is AppEvent.SaveTunnel -> saveTunnel(event.tunnel)
                }
            }
        }
    }

    private suspend fun saveTunnelsUniquely(tunnels: List<TunnelConf>) {
        withContext(ioDispatcher) {
            tunnelMutex.withLock {
                val existingTunnels = appDataRepository.tunnels.getAll()
                val uniqueTuns = generateUniquelyNamedConfigs(tunnels, existingTunnels)
                appDataRepository.tunnels.saveAll(uniqueTuns)
            }
        }
    }

    private fun generateUniquelyNamedConfigs(
        incoming: List<TunnelConf>,
        existing: List<TunnelConf>,
    ): List<TunnelConf> {
        val usedNames = existing.map { it.tunName }.toMutableSet()
        val result = mutableListOf<TunnelConf>()

        for (tun in incoming) {
            var uniqueName = tun.tunName
            var counter = 1

            while (uniqueName in usedNames) {
                uniqueName = "${tun.tunName} ($counter)"
                counter++
            }

            usedNames.add(uniqueName)
            result.add(tun.copy(tunName = uniqueName))
        }

        return result
    }

    fun handleUiEvent(event: UiEvent): Job =
        viewModelScope.launch(mainDispatcher) { _uiEvent.emit(event) }

    fun handleEvent(event: AppEvent) {
        _eventChannel.trySend(event)
    }

    private suspend fun handleTogglePingMonitoring(appSettings: AppSettings) {
        saveSettings(appSettings.copy(isPingEnabled = !appSettings.isPingEnabled))
    }

    private suspend fun handleMonitoringSaveChanges(
        appSettings: AppSettings,
        pingInterval: Int,
        tunnelPingAttempts: Int,
        pingTimeout: Int?,
    ) {
        saveSettings(
            appSettings.copy(
                tunnelPingIntervalSeconds = pingInterval,
                tunnelPingAttempts = tunnelPingAttempts,
                tunnelPingTimeoutSeconds = pingTimeout,
            )
        )
    }

    private suspend fun handleToggleShowDetailedPingStats(currentAppState: AppState) {
        appDataRepository.appState.setShowDetailedPingStats(!currentAppState.showDetailedPingStats)
    }

    private suspend fun saveAllTunnels(tunnels: List<TunnelConf>) {
        appDataRepository.tunnels.saveAll(tunnels)
    }

    private suspend fun handleSetDetectionMethod(
        detectionMethod: AndroidNetworkMonitor.WifiDetectionMethod,
        appSettings: AppSettings,
    ) {
        if (detectionMethod == appSettings.wifiDetectionMethod) return
        when (detectionMethod) {
            AndroidNetworkMonitor.WifiDetectionMethod.ROOT -> if (!requestRoot()) return
            AndroidNetworkMonitor.WifiDetectionMethod.SHIZUKU -> {
                Shizuku.addRequestPermissionResultListener(
                    Shizuku.OnRequestPermissionResultListener { requestCode: Int, grantResult: Int
                        ->
                        if (grantResult != PERMISSION_GRANTED)
                            return@OnRequestPermissionResultListener
                        viewModelScope.launch {
                            saveSettings(appSettings.copy(wifiDetectionMethod = detectionMethod))
                        }
                    }
                )
                try {
                    if (Shizuku.checkSelfPermission() != PERMISSION_GRANTED)
                        return Shizuku.requestPermission(123)
                } catch (e: Exception) {
                    Timber.e(e)
                    return handleShowMessage(
                        StringValue.StringResource(R.string.shizuku_not_detected)
                    )
                }
            }
            else -> Unit
        }
        saveSettings(appSettings.copy(wifiDetectionMethod = detectionMethod))
    }

    private fun handleToggleSelectAllTunnels(tunnels: List<TunnelConf>) =
        _appViewState.update { it ->
            val remove = tunnels.size == it.selectedTunnels.size
            it.copy(
                selectedTunnels =
                    it.selectedTunnels.toMutableList().apply {
                        if (remove) removeAll(tunnels)
                        else addAllUnique(tunnels) { existing, new -> existing.id == new.id }
                    }
            )
        }

    private fun handleToggleSelectedTunnel(tunnel: TunnelConf) =
        _appViewState.update {
            it.copy(
                selectedTunnels =
                    it.selectedTunnels.toMutableList().apply {
                        if (it.selectedTunnels.contains(tunnel)) remove(tunnel) else add(tunnel)
                    }
            )
        }

    private suspend fun handleToggleTunnelStats(tunnelId: Int, appState: AppState) {
        if (appState.expandedTunnelIds.contains(tunnelId)) {
            appDataRepository.appState.removeTunnelExpanded(tunnelId)
        } else {
            appDataRepository.appState.setTunnelExpanded(tunnelId)
        }
    }

    private suspend fun handleToggleRemoteControl(appState: AppState) {
        val enabled = !appState.isRemoteControlEnabled
        if (enabled) appDataRepository.appState.setRemoteKey(UUID.randomUUID().toString())
        appDataRepository.appState.setIsRemoteControlEnabled(enabled)
    }

    private fun startCollectingLogs() =
        viewModelScope.launch {
            logReader.bufferedLogs.flowOn(ioDispatcher).collect { logMessage ->
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

    private fun handleTunnelMessages() =
        viewModelScope.launch {
            launch {
                tunnelManager.errorEvents.collect { errorEvent ->
                    handleShowMessage(
                        when (val event = errorEvent.second) {
                            is BackendError.BounceFailed -> event.toStringValue()
                            else ->
                                StringValue.StringResource(
                                    R.string.tunnel_error_template,
                                    errorEvent.second.toStringRes(),
                                )
                        }
                    )
                }
            }
            launch {
                tunnelManager.messageEvents.collect { messageEvent ->
                    handleShowMessage(messageEvent.second.toStringValue())
                }
            }
        }

    private suspend fun handleAppReadyCheck(tunnels: List<TunnelConf>) {
        if (tunnels.size == appDataRepository.tunnels.count()) {
            _appViewState.update { it.copy(isAppReady = true) }
        }
    }

    private fun handleSetBottomSheet(bottomSheet: AppViewState.BottomSheet) =
        _appViewState.update { it.copy(bottomSheet = bottomSheet) }

    private suspend fun handleTunnelPingTargetChange(tunnelConf: TunnelConf, target: String) =
        saveTunnel(tunnelConf.copy(pingTarget = target.ifBlank { null }))

    private suspend fun handleTogglePingTunnel(tunnel: TunnelConf) =
        saveTunnel(tunnel.copy(restartOnPingFailure = !tunnel.restartOnPingFailure))

    private suspend fun handleToggleLocalLogging(currentlyEnabled: Boolean) {
        loggerMutex.withLock {
            val enable = !currentlyEnabled
            appDataRepository.appState.setLocalLogsEnabled(enable)
            if (enable) {
                logsJob?.cancel()
                logReader.start()
                logsJob = startCollectingLogs()
            } else {
                logReader.stop()
                logsJob?.cancel()
                _logs.update { emptyList() }
            }
        }
    }

    private suspend fun handleSetDebounceDelay(appSettings: AppSettings, delay: Int) =
        saveSettings(appSettings.copy(debounceDelaySeconds = delay))

    private suspend fun handleCopySelectedTunnel(existingTunnels: List<TunnelConf>) {
        val tunnel = _appViewState.value.selectedTunnels.firstOrNull() ?: return
        saveTunnel(
            TunnelConf(
                tunName = tunnel.generateUniqueName(existingTunnels.map { it.tunName }),
                wgQuick = tunnel.wgQuick,
                amQuick = tunnel.amQuick,
            )
        )
        clearSelectedTunnels()
    }

    private fun clearSelectedTunnels() =
        _appViewState.update {
            it.copy(selectedTunnels = it.selectedTunnels.toMutableList().apply { clear() })
        }

    private suspend fun handleDeleteSelectedTunnels() =
        _appViewState.value.selectedTunnels
            .forEach {
                appDataRepository.tunnels.delete(it)
                appDataRepository.appState.removeTunnelExpanded(it.id)
            }
            .also { clearSelectedTunnels() }

    private fun requestVpnPermission(request: Boolean) =
        _appViewState.update { it.copy(requestVpnPermission = request) }

    private fun requestBatteryPermission(request: Boolean) =
        _appViewState.update { it.copy(requestBatteryPermission = request) }

    private suspend fun handleStartTunnel(tunnel: TunnelConf, appSettings: AppSettings) {
        clearSelectedTunnels()
        tunControlMutex.withLock {
            if (!tunnelManager.hasVpnPermission() && !appSettings.isKernelEnabled)
                return@withLock requestVpnPermission(true)
            tunnelManager.startTunnel(tunnel)
        }
    }

    private suspend fun handleStopTunnel(tunnel: TunnelConf) {
        clearSelectedTunnels()
        tunControlMutex.withLock { tunnelManager.stopTunnel(tunnel) }
    }

    private suspend fun handleToggleAutoTunnel(state: AppUiState) {
        tunControlMutex.withLock {
            if (
                !state.appSettings.isAutoTunnelEnabled &&
                    !tunnelManager.hasVpnPermission() &&
                    !state.appSettings.isKernelEnabled
            ) {
                return@withLock requestVpnPermission(true)
            }
            if (!state.appState.isBatteryOptimizationDisableShown)
                return@withLock requestBatteryPermission(true)
            serviceManager.toggleAutoTunnel()
        }
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

    private suspend fun handleImportTunnelFromFile(uri: Uri) {
        runCatching {
                val tunnelConfigs = fileUtils.buildTunnelsFromUri(uri)
                saveTunnelsUniquely(tunnelConfigs)
            }
            .onFailure {
                when (it) {
                    is FileReadException,
                    is BadConfigException ->
                        handleShowMessage(StringValue.StringResource(R.string.error_file_format))
                    is InvalidFileExtensionException ->
                        handleShowMessage(StringValue.StringResource(R.string.error_file_extension))
                    else -> handleShowMessage(StringValue.StringResource(R.string.unknown_error))
                }
                Timber.e(it)
            }
    }

    private suspend fun handleClipboardImport(config: String, tunnels: List<TunnelConf>) {
        runCatching {
                val amConfig = TunnelConf.configFromAmQuick(config)
                val tunnelConf = TunnelConf.tunnelConfigFromAmConfig(amConfig)
                saveTunnelsUniquely(listOf(tunnelConf))
            }
            .onFailure {
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
                    saveTunnelsUniquely(listOf(tunnelConf))
                }
            }
            .onFailure {
                Timber.e(it)
                val message =
                    when (it) {
                        is InvalidFileExtensionException ->
                            StringValue.StringResource(R.string.error_file_extension)
                        else -> StringValue.StringResource(R.string.error_download_failed)
                    }
                handleShowMessage(message)
            }
    }

    private suspend fun handleImportTunnelFromQr(
        result: String,
        existingTunnels: List<TunnelConf>,
    ) {
        handleClipboardImport(result, existingTunnels)
        popBackStack()
    }

    private suspend fun setBatteryOptimizeDisableShown() {
        requestBatteryPermission(false)
        appDataRepository.appState.setBatteryOptimizationDisableShown(true)
    }

    private fun initServicesFromSavedState(state: AppUiState) =
        viewModelScope.launch(ioDispatcher) {
            tunControlMutex.withLock {
                if (state.appSettings.isAutoTunnelEnabled) serviceManager.startAutoTunnel()
                state.tunnels.filter { it.isActive }.forEach { tunnelManager.startTunnel(it) }
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

    private suspend fun handleToggleAlwaysOnVPN(appSettings: AppSettings) =
        saveSettings(appSettings.copy(isAlwaysOnVpnEnabled = !appSettings.isAlwaysOnVpnEnabled))

    private suspend fun handleLocaleChange(localeTag: String) {
        withContext(mainDispatcher) { LocaleUtil.changeLocale(localeTag) }
        appDataRepository.appState.setLocale(localeTag)
        _appViewState.update { it.copy(isConfigChanged = true) }
    }

    private suspend fun handleToggleRestartAtBoot(appSettings: AppSettings) =
        saveSettings(appSettings.copy(isRestoreOnBootEnabled = !appSettings.isRestoreOnBootEnabled))

    private suspend fun handleToggleVpnKillSwitch(appSettings: AppSettings) {
        val enabled = !appSettings.isVpnKillSwitchEnabled
        if (enabled && !tunnelManager.hasVpnPermission()) return requestVpnPermission(true)
        val updatedSettings =
            appSettings.copy(
                isVpnKillSwitchEnabled = enabled,
                isLanOnKillSwitchEnabled =
                    if (enabled) appSettings.isLanOnKillSwitchEnabled else false,
            )
        saveSettings(updatedSettings)
        handleKillSwitchChange(updatedSettings)
    }

    private suspend fun handleToggleLanOnKillSwitch(appSettings: AppSettings) {
        val updatedSettings =
            appSettings.copy(isLanOnKillSwitchEnabled = !appSettings.isLanOnKillSwitchEnabled)
        saveSettings(updatedSettings)
        handleKillSwitchChange(updatedSettings)
    }

    private fun handleKillSwitchChange(appSettings: AppSettings) {
        // let auto tunnel handle kill switch changes if running
        if (uiState.value.isAutoTunnelActive) return
        if (!appSettings.isVpnKillSwitchEnabled)
            return tunnelManager.setBackendStatus(BackendStatus.Active)
        Timber.d("Starting kill switch")
        val allowedIps =
            if (appSettings.isLanOnKillSwitchEnabled) TunnelConf.LAN_BYPASS_ALLOWED_IPS
            else emptyList()
        tunnelManager.setBackendStatus(BackendStatus.KillSwitch(allowedIps))
    }

    private suspend fun handleToggleAppShortcuts(appSettings: AppSettings) {
        val enabled = !appSettings.isShortcutsEnabled
        if (enabled) shortcutManager.addShortcuts() else shortcutManager.removeShortcuts()
        saveSettings(appSettings.copy(isShortcutsEnabled = enabled))
    }

    private suspend fun handleTogglePrimaryTunnel(tunnelConf: TunnelConf) {
        tunnelMutex.withLock {
            appDataRepository.tunnels.updatePrimaryTunnel(
                when (tunnelConf.isPrimaryTunnel) {
                    true -> null
                    false -> tunnelConf
                }
            )
        }
    }

    private suspend fun handleToggleIpv4(tunnelConf: TunnelConf) =
        saveTunnel(tunnelConf.copy(isIpv4Preferred = !tunnelConf.isIpv4Preferred))

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
        tunnelManager.setBackendStatus(BackendStatus.Inactive)
        saveSettings(
            appSettings.copy(
                isKernelEnabled = enabled,
                isVpnKillSwitchEnabled = false,
                isLanOnKillSwitchEnabled = false,
            )
        )
    }

    private suspend fun handleRemoveTunnelRunSSID(ssid: String, tunnelConfig: TunnelConf) =
        saveTunnel(
            tunnelConfig.copy(tunnelNetworks = (tunnelConfig.tunnelNetworks - ssid).toMutableList())
        )

    private suspend fun handleAddTunnelRunSSID(
        ssid: String,
        tunnelConf: TunnelConf,
        existingTunnels: List<TunnelConf>,
    ) {
        if (ssid.isBlank()) return
        val trimmed = ssid.trim()
        if (existingTunnels.any { it.tunnelNetworks.contains(trimmed) })
            return handleShowMessage(StringValue.StringResource(R.string.error_ssid_exists))
        saveTunnel(
            tunnelConf.copy(tunnelNetworks = (tunnelConf.tunnelNetworks + ssid).toMutableList())
        )
    }

    private suspend fun handleToggleMobileDataTunnel(tunnelConf: TunnelConf) {
        tunnelMutex.withLock {
            if (tunnelConf.isMobileDataTunnel)
                return appDataRepository.tunnels.updateMobileDataTunnel(null)
            appDataRepository.tunnels.updateMobileDataTunnel(tunnelConf)
        }
    }

    private suspend fun handleToggleEthernetTunnel(tunnelConf: TunnelConf) {
        tunnelMutex.withLock {
            if (tunnelConf.isEthernetTunnel)
                return appDataRepository.tunnels.updateEthernetTunnel(null)
            appDataRepository.tunnels.updateEthernetTunnel(tunnelConf)
        }
    }

    private suspend fun handleToggleAutoTunnelOnWifi(appSettings: AppSettings) =
        saveSettings(appSettings.copy(isTunnelOnWifiEnabled = !appSettings.isTunnelOnWifiEnabled))

    private suspend fun handleToggleAutoTunnelOnCellular(appSettings: AppSettings) =
        saveSettings(
            appSettings.copy(isTunnelOnMobileDataEnabled = !appSettings.isTunnelOnMobileDataEnabled)
        )

    private suspend fun handleToggleAutoTunnelWildcards(appSettings: AppSettings) =
        saveSettings(appSettings.copy(isWildcardsEnabled = !appSettings.isWildcardsEnabled))

    private suspend fun handleDeleteTrustedSSID(ssid: String, appSettings: AppSettings) =
        saveSettings(
            appSettings.copy(
                trustedNetworkSSIDs = (appSettings.trustedNetworkSSIDs - ssid).toMutableList()
            )
        )

    private suspend fun handleToggleTunnelOnEthernet(appSettings: AppSettings) =
        saveSettings(
            appSettings.copy(isTunnelOnEthernetEnabled = !appSettings.isTunnelOnEthernetEnabled)
        )

    private suspend fun handleSaveTrustedSSID(ssid: String, appSettings: AppSettings) {
        if (ssid.isEmpty()) return
        val trimmed = ssid.trim()
        if (appSettings.trustedNetworkSSIDs.contains(trimmed))
            return handleShowMessage(StringValue.StringResource(R.string.error_ssid_exists))
        saveSettings(
            appSettings.copy(
                trustedNetworkSSIDs = (appSettings.trustedNetworkSSIDs + ssid).toMutableList()
            )
        )
    }

    private suspend fun handleToggleStopOnNoInternet(appSettings: AppSettings) =
        saveSettings(
            appSettings.copy(isStopOnNoInternetEnabled = !appSettings.isStopOnNoInternetEnabled)
        )

    private suspend fun handleToggleStopKillSwitchOnTrusted(appSettings: AppSettings) =
        saveSettings(
            appSettings.copy(
                isDisableKillSwitchOnTrustedEnabled =
                    !appSettings.isDisableKillSwitchOnTrustedEnabled
            )
        )

    private suspend fun isKernelSupported(): Boolean {
        return withContext(ioDispatcher) { WgQuickBackend.hasKernelSupport() }
    }

    private suspend fun saveSettings(appSettings: AppSettings) =
        withContext(ioDispatcher) {
            settingsMutex.withLock { appDataRepository.settings.save(appSettings) }
        }

    private suspend fun saveTunnel(tunnel: TunnelConf) =
        withContext(ioDispatcher) {
            tunnelMutex.withLock { appDataRepository.tunnels.save(tunnel) }
        }

    private suspend fun handleExportSelectedTunnels(configType: ConfigType, uri: Uri?) {
        val tunnels = _appViewState.value.selectedTunnels
        try {
            if (tunnels.isEmpty()) return
            val (files, shareFileName) =
                when (configType) {
                    ConfigType.AM -> {
                        val amFiles = fileUtils.createAmFiles(tunnels)
                        if (amFiles.isEmpty()) {
                            throw IOException("No valid Amnezia config files created")
                        }
                        Pair(amFiles, "am-export_${Instant.now().epochSecond}.zip")
                    }
                    ConfigType.WG -> {
                        val wgFiles = fileUtils.createWgFiles(tunnels)
                        if (wgFiles.isEmpty()) {
                            throw IOException("No valid WireGuard config files created")
                        }
                        Pair(wgFiles, "wg-export_${Instant.now().epochSecond}.zip")
                    }
                }

            val shareFile = fileUtils.createNewShareFile(shareFileName)
            fileUtils.zipAll(shareFile, files)
            if (!shareFile.exists() || shareFile.length() == 0L) {
                throw IOException("Zip file is empty or not created: ${shareFile.path}")
            }

            // fall back to save to downloads for older devices
            if (uri != null) {
                val copyResult = fileUtils.copyFileToUri(shareFile, uri)
                copyResult.fold(
                    onSuccess = {
                        handleShowMessage(StringValue.StringResource(R.string.export_success))
                    },
                    onFailure = { error ->
                        Timber.w("User likely cancelled or file write failed: ${error.message}")
                        handleShowMessage(StringValue.StringResource(R.string.export_failed))
                    },
                )
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    fileUtils.saveToDownloadsWithMediaStore(shareFile, Constants.ZIP_FILE_MIME_TYPE)
                    handleShowMessage(StringValue.StringResource(R.string.export_success))
                } else throw IOException("File exporting not supported on this device")
            }
        } catch (e: Exception) {
            Timber.e(e, "Export failed")
            handleShowMessage(StringValue.StringResource(R.string.export_failed))
        } finally {
            handleSetBottomSheet(AppViewState.BottomSheet.NONE)
            clearSelectedTunnels()
        }
    }

    private suspend fun handleExportLogs() {
        runCatching {
                val file =
                    fileUtils.createNewShareFile(
                        "${Constants.BASE_LOG_FILE_NAME}-${Instant.now().epochSecond}.zip"
                    )
                logReader.zipLogFiles(file.absolutePath)
                fileUtils.shareFile(file)
            }
            .onFailure {
                Timber.e(it)
                handleShowMessage(StringValue.StringResource(R.string.export_failed))
            }
    }

    private suspend fun handleDeleteLogs() {
        loggerMutex.withLock {
            logsJob?.cancel()
            _logs.update { emptyList() }
            logReader.stop()
            logReader.deleteAndClearLogs()
            logReader.start()
            logsJob = startCollectingLogs()
        }
    }

    private suspend fun requestRoot(): Boolean {
        return withContext(ioDispatcher) {
            try {
                rootShell.get().start()
                handleShowMessage(StringValue.StringResource(R.string.root_accepted))
                true
            } catch (_: Exception) {
                handleShowMessage(StringValue.StringResource(R.string.error_root_denied))
                false
            }
        }
    }
}
