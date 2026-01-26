package com.zaneschepke.wireguardautotunnel.di

import android.content.Context
import android.os.PowerManager
import com.zaneschepke.logcatter.LogReader
import com.zaneschepke.logcatter.LogcatReader
import com.zaneschepke.wireguardautotunnel.core.notification.NotificationManager
import com.zaneschepke.wireguardautotunnel.core.notification.NotificationMonitor
import com.zaneschepke.wireguardautotunnel.core.notification.WireGuardNotification
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.core.shortcut.DynamicShortcutManager
import com.zaneschepke.wireguardautotunnel.core.shortcut.ShortcutManager
import com.zaneschepke.wireguardautotunnel.data.AppDatabase
import com.zaneschepke.wireguardautotunnel.data.repository.RoomLockdownSettingsRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.GlobalEffectRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.LockdownSettingsRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.SelectedTunnelsRepository
import com.zaneschepke.wireguardautotunnel.util.FileUtils
import com.zaneschepke.wireguardautotunnel.util.network.NetworkUtils
import com.zaneschepke.wireguardautotunnel.viewmodel.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

@OptIn(KoinExperimentalAPI::class)
val appModule = module {
    single<CoroutineScope>(named(Scope.APPLICATION)) {
        CoroutineScope(SupervisorJob() + get<CoroutineDispatcher>(named(Dispatcher.DEFAULT)))
    }

    single<LogReader> { LogcatReader.init(storageDir = androidContext().filesDir.absolutePath) }

    single<PowerManager> {
        androidContext().getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    singleOf(::NotificationMonitor)
    singleOf(::WireGuardNotification) bind NotificationManager::class

    single {
        ServiceManager(
            androidContext(),
            get(named(Dispatcher.IO)),
            get(named(Scope.APPLICATION)),
            get(named(Dispatcher.MAIN)),
            get(),
        )
    }

    singleOf(::GlobalEffectRepository)

    single { FileUtils(androidContext(), get(named(Dispatcher.IO))) }
    single<ShortcutManager> { DynamicShortcutManager(androidContext(), get(named(Dispatcher.IO))) }
    singleOf(::SelectedTunnelsRepository)

    single { NetworkUtils(get(named(Dispatcher.IO))) }

    // --- REPOSITORIES ADDITIONNELS ---
    single { get<AppDatabase>().lockdownSettingsDao() }
    single<LockdownSettingsRepository> { RoomLockdownSettingsRepository(get()) }

    single {
        com.zaneschepke.wireguardautotunnel.core.service.autotunnel.handler
            .AutoTunnelRoamingHandler(
                context = androidContext(),
                ioDispatcher = get(named(Dispatcher.IO)),
                tunnelManager = get(),
                networkMonitor = get(),
                settingsRepository = get(),
            )
    }

    // --- VIEWMODELS ---
    viewModelOf(::AutoTunnelViewModel)
    viewModel { (id: Int?) -> ConfigViewModel(get(), get(), get(), id) }
    viewModelOf(::DnsViewModel)
    viewModelOf(::LicenseViewModel)
    viewModelOf(::LockdownViewModel)
    viewModelOf(::LoggerViewModel)
    viewModelOf(::MonitoringViewModel)
    viewModelOf(::ProxySettingsViewModel)
    viewModelOf(::SettingsViewModel)

    // SharedAppViewModel with Context to track manual actions
    viewModel {
        SharedAppViewModel(
            appContext = androidContext(),
            appStateRepository = get(),
            serviceManager = get(),
            tunnelManager = get(),
            globalEffectRepository = get(),
            tunnelRepository = get(),
            settingsRepository = get(),
            selectedTunnelsRepository = get(),
            monitoringSettingsRepository = get(),
            rootShellUtils = get(),
            httpClient = get(),
            fileUtils = get(),
        )
    }

    viewModel { (id: Int) -> SplitTunnelViewModel(get(), get(), get(), id) }
    viewModel { SupportViewModel(get(), get(named(Dispatcher.MAIN)), get()) }
    viewModel { (id: Int) -> TunnelViewModel(get(), get(), id) }
}
