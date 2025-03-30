package com.zaneschepke.wireguardautotunnel.ui.screens.main.splittunnel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarController
import com.zaneschepke.wireguardautotunnel.ui.screens.main.splittunnel.state.SplitOption
import com.zaneschepke.wireguardautotunnel.ui.screens.main.splittunnel.state.SplitTunnelUiState
import com.zaneschepke.wireguardautotunnel.ui.screens.main.splittunnel.state.TunnelApp
import com.zaneschepke.wireguardautotunnel.ui.state.ConfigProxy
import com.zaneschepke.wireguardautotunnel.ui.state.InterfaceProxy
import com.zaneschepke.wireguardautotunnel.util.StringValue
import com.zaneschepke.wireguardautotunnel.util.extensions.getAllInternetCapablePackages
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SplitTunnelViewModel @Inject constructor(
	@ApplicationContext private val context: Context,
	private val tunnelRepository: TunnelRepository,
	savedStateHandle: SavedStateHandle,
) : ViewModel() {

	private val _uiState = MutableStateFlow(SplitTunnelUiState())
	val uiState: StateFlow<SplitTunnelUiState> = _uiState
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5000L),
			initialValue = SplitTunnelUiState(),
		)

	private val tunnelId: Int? = savedStateHandle.get<Int>(Route.SplitTunnel.KEY_ID)
	private var allTunneledApps: List<Pair<TunnelApp, Boolean>> = emptyList()

	init {
		tunnelId?.let { loadInitialState(it) }
	}

	private fun loadInitialState(tunnelId: Int) = viewModelScope.launch {
		val tunnel = tunnelRepository.getById(tunnelId) ?: return@launch
		val proxyInterface = InterfaceProxy.from(tunnel.toAmConfig().`interface`)
		val splitOption = when {
			proxyInterface.excludedApplications.isNotEmpty() -> SplitOption.EXCLUDE
			proxyInterface.includedApplications.isNotEmpty() -> SplitOption.INCLUDE
			else -> SplitOption.ALL
		}

		val packages = context.getAllInternetCapablePackages()

		val installedPackages = packages
			.map { it.packageName }
			.toSet()

		// remove uninstalled apps
		proxyInterface.includedApplications.retainAll { it in installedPackages }
		proxyInterface.excludedApplications.retainAll { it in installedPackages }

		var configProxy = ConfigProxy.from(tunnel.toAmConfig())
		configProxy = configProxy.copy(`interface` = proxyInterface)
		saveProxyConfig(configProxy, tunnel)

		val collator = Collator.getInstance(Locale.getDefault())
		val tunneledApps = packages
			.filter { it.applicationInfo != null }
			.map { pack ->
				val selected = when (splitOption) {
					SplitOption.INCLUDE -> proxyInterface.includedApplications.contains(pack.packageName)
					SplitOption.ALL -> false
					SplitOption.EXCLUDE -> proxyInterface.excludedApplications.contains(pack.packageName)
				}
				Pair(
					TunnelApp(
						name = context.packageManager.getApplicationLabel(pack.applicationInfo!!).toString(),
						`package` = pack.packageName,
					),
					selected,
				)
			}.sortedWith(compareBy(collator) { it.first.name })

		allTunneledApps = tunneledApps

		delay(500)

		_uiState.update {
			SplitTunnelUiState(
				loading = false,
				tunnelConf = tunnel,
				tunneledApps = tunneledApps,
				splitOption = splitOption,
			)
		}
	}

	fun onSearchQuery(query: String) {
		val filteredApps = if (query.isBlank()) {
			allTunneledApps
		} else {
			allTunneledApps.filter {
				it.first.name.contains(query, ignoreCase = true) ||
					it.first.`package`.contains(query, ignoreCase = true)
			}
		}
		_uiState.update {
			it.copy(
				searchQuery = query,
				tunneledApps = filteredApps,
			)
		}
	}

	fun updateSplitOption(newOption: SplitOption) {
		_uiState.value = _uiState.value.copy(splitOption = newOption)
	}

	fun toggleAppSelection(packageName: String) {
		val currentState = _uiState.value
		val updatedApps = currentState.tunneledApps.map { (app, selected) ->
			if (app.`package` == packageName) Pair(app, !selected) else Pair(app, selected)
		}
		_uiState.value = currentState.copy(tunneledApps = updatedApps)
	}

	fun saveChanges() = viewModelScope.launch {
		val state = _uiState.value
		val tunnel = state.tunnelConf ?: return@launch
		val configProxy = ConfigProxy.from(tunnel.toAmConfig())
		val updatedApps = state.tunneledApps
		with(configProxy.`interface`) {
			includedApplications.clear()
			excludedApplications.clear()
			when (state.splitOption) {
				SplitOption.INCLUDE -> {
					includedApplications.addAll(updatedApps.filter { it.second }.map { it.first.`package` })
				}
				SplitOption.EXCLUDE -> {
					excludedApplications.addAll(updatedApps.filter { it.second }.map { it.first.`package` })
				}
				SplitOption.ALL -> Unit
			}
		}
		saveProxyConfig(configProxy, tunnel)
		SnackbarController.showMessage(StringValue.StringResource(R.string.config_changes_saved))
	}

	private suspend fun saveProxyConfig(proxy: ConfigProxy, tunnel: TunnelConf) {
		val (wg, am) = proxy.buildConfigs()
		tunnelRepository.save(tunnel.copyWithCallback(amQuick = am.toAwgQuickString(true), wgQuick = wg.toWgQuickString(true)))
	}
}
