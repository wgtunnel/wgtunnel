package com.zaneschepke.wireguardautotunnel

import android.app.Application
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import com.zaneschepke.logcatter.LogReader
import com.zaneschepke.wireguardautotunnel.data.repository.AppStateRepository
import com.zaneschepke.wireguardautotunnel.data.repository.SettingsRepository
import com.zaneschepke.wireguardautotunnel.module.ApplicationScope
import com.zaneschepke.wireguardautotunnel.module.IoDispatcher
import com.zaneschepke.wireguardautotunnel.module.MainDispatcher
import com.zaneschepke.wireguardautotunnel.service.tunnel.BackendState
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelService
import com.zaneschepke.wireguardautotunnel.util.LocaleUtil
import com.zaneschepke.wireguardautotunnel.util.ReleaseTree
import com.zaneschepke.wireguardautotunnel.util.extensions.isRunningOnTv
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class WireGuardAutoTunnel : Application() {

	@Inject
	@ApplicationScope
	lateinit var applicationScope: CoroutineScope

	@Inject
	lateinit var logReader: LogReader

	@Inject
	lateinit var appStateRepository: AppStateRepository

	@Inject
	lateinit var settingsRepository: SettingsRepository

	@Inject
	lateinit var tunnelService: TunnelService

	@Inject
	@IoDispatcher
	lateinit var ioDispatcher: CoroutineDispatcher

	@Inject
	@MainDispatcher
	lateinit var mainDispatcher: CoroutineDispatcher

	override fun onCreate() {
		super.onCreate()
		instance = this
		if (BuildConfig.DEBUG) {
			Timber.plant(Timber.DebugTree())
			StrictMode.setThreadPolicy(
				ThreadPolicy.Builder()
					.detectDiskReads()
					.detectDiskWrites()
					.detectNetwork()
					.penaltyLog()
					.build(),
			)
		} else {
			Timber.plant(ReleaseTree())
		}

		applicationScope.launch {
			withContext(mainDispatcher) {
				if (appStateRepository.isLocalLogsEnabled() && !isRunningOnTv()) logReader.initialize()
			}
			if (!settingsRepository.getSettings().isKernelEnabled) {
				tunnelService.setBackendState(BackendState.SERVICE_ACTIVE, emptyList())
			}

			appStateRepository.getLocale()?.let {
				LocaleUtil.changeLocale(it)
			}
		}
	}

	override fun onTerminate() {
		applicationScope.launch {
			tunnelService.setBackendState(BackendState.INACTIVE, emptyList())
		}
		super.onTerminate()
	}

	companion object {
		lateinit var instance: WireGuardAutoTunnel
			private set
	}
}
