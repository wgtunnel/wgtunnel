package com.zaneschepke.wireguardautotunnel.di

import android.content.Context
import com.wireguard.android.backend.WgQuickBackend
import com.wireguard.android.util.RootShell
import com.wireguard.android.util.ToolsInstaller
import com.zaneschepke.logcatter.LogReader
import com.zaneschepke.networkmonitor.AndroidNetworkMonitor
import com.zaneschepke.networkmonitor.NetworkMonitor
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.core.tunnel.*
import com.zaneschepke.wireguardautotunnel.domain.repository.GeneralSettingRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.ProxySettingsRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.util.extensions.to
import com.zaneschepke.wireguardautotunnel.util.network.NetworkUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import org.amnezia.awg.backend.Backend
import org.amnezia.awg.backend.GoBackend
import org.amnezia.awg.backend.ProxyGoBackend
import org.amnezia.awg.backend.RootTunnelActionHandler

@Module
@InstallIn(SingletonComponent::class)
class TunnelModule {

    @Provides
    @Singleton
    @TunnelShell
    fun provideTunnelRootShell(@ApplicationContext context: Context): RootShell {
        return RootShell(context)
    }

    @Provides
    @Singleton
    @AppShell
    fun provideAppRootShell(@ApplicationContext context: Context): RootShell {
        return RootShell(context)
    }

    @Provides
    @Singleton
    @Userspace
    fun provideAmneziaBackend(@ApplicationContext context: Context): Backend {
        return GoBackend(context, RootTunnelActionHandler(org.amnezia.awg.util.RootShell(context)))
    }

    @Provides
    @Singleton
    @ProxyUserspace
    fun provideAmneziaProxyBackend(@ApplicationContext context: Context): Backend {
        return ProxyGoBackend(
            context,
            RootTunnelActionHandler(org.amnezia.awg.util.RootShell(context)),
        )
    }

    @Provides
    @Singleton
    fun provideKernelBackend(
        @ApplicationContext context: Context,
        @TunnelShell shell: RootShell,
    ): com.wireguard.android.backend.Backend {
        return WgQuickBackend(
                context,
                shell,
                ToolsInstaller(context, shell),
                com.wireguard.android.backend.RootTunnelActionHandler(shell),
            )
            .also { it.setMultipleTunnels(true) }
    }

    @Provides
    @Singleton
    @Kernel
    fun provideKernelProvider(
        @ApplicationScope applicationScope: CoroutineScope,
        backend: com.wireguard.android.backend.Backend,
    ): TunnelProvider {
        return KernelTunnel(applicationScope, backend)
    }

    @Provides
    @Singleton
    @Userspace
    fun provideUserspaceProvider(
        @ApplicationScope applicationScope: CoroutineScope,
        proxySettingsRepository: ProxySettingsRepository,
        settingsRepository: GeneralSettingRepository,
        @Userspace backend: Backend,
    ): TunnelProvider {
        return UserspaceTunnel(
            applicationScope,
            proxySettingsRepository,
            settingsRepository,
            backend,
        )
    }

    @Provides
    @Singleton
    @ProxyUserspace
    fun provideProxyUserspaceProvider(
        @ApplicationScope applicationScope: CoroutineScope,
        settingsRepository: GeneralSettingRepository,
        proxySettingsRepository: ProxySettingsRepository,
        @ProxyUserspace backend: Backend,
    ): TunnelProvider {
        return UserspaceTunnel(
            applicationScope,
            proxySettingsRepository,
            settingsRepository,
            backend,
        )
    }

    @Provides
    @Singleton
    fun provideTunnelManager(
        @Kernel kernelTunnel: TunnelProvider,
        @Userspace userspaceTunnel: TunnelProvider,
        @ProxyUserspace proxyTunnel: TunnelProvider,
        serviceManager: ServiceManager,
        tunnelRepository: TunnelRepository,
        settingsRepository: GeneralSettingRepository,
        tunnelMonitor: TunnelMonitor,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @ApplicationScope applicationScope: CoroutineScope,
    ): TunnelManager {
        return TunnelManager(
            kernelTunnel,
            userspaceTunnel,
            proxyTunnel,
            serviceManager,
            settingsRepository,
            tunnelRepository,
            tunnelMonitor,
            applicationScope,
            ioDispatcher,
        )
    }

    @Provides
    @Singleton
    fun provideNetworkMonitor(
        @ApplicationContext context: Context,
        settingsRepository: GeneralSettingRepository,
        @ApplicationScope applicationScope: CoroutineScope,
        @AppShell appShell: RootShell,
    ): NetworkMonitor {
        return AndroidNetworkMonitor(
            context,
            object : AndroidNetworkMonitor.ConfigurationListener {
                override val detectionMethod: Flow<AndroidNetworkMonitor.WifiDetectionMethod>
                    get() =
                        settingsRepository.flow
                            .distinctUntilChangedBy { it.wifiDetectionMethod }
                            .map { it.wifiDetectionMethod.to() }

                override val rootShell: RootShell
                    get() = appShell
            },
            applicationScope,
        )
    }

    @Singleton
    @Provides
    fun provideServiceManager(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @MainDispatcher mainCoroutineDispatcher: CoroutineDispatcher,
        @ApplicationScope applicationScope: CoroutineScope,
        settingsRepository: GeneralSettingRepository,
    ): ServiceManager {
        return ServiceManager(
            context,
            ioDispatcher,
            applicationScope,
            mainCoroutineDispatcher,
            settingsRepository,
        )
    }

    @Singleton
    @Provides
    fun provideTunnelMonitor(
        networkMonitor: NetworkMonitor,
        networkUtils: NetworkUtils,
        logReader: LogReader,
        tunnelsRepository: TunnelRepository,
        settingsRepository: GeneralSettingRepository,
    ): TunnelMonitor {
        return TunnelMonitor(
            settingsRepository,
            tunnelsRepository,
            networkMonitor,
            networkUtils,
            logReader,
        )
    }
}
