package com.zaneschepke.wireguardautotunnel.di

import android.content.Context
import com.wireguard.android.backend.WgQuickBackend
import com.wireguard.android.util.RootShell
import com.wireguard.android.util.ToolsInstaller
import com.zaneschepke.networkmonitor.AndroidNetworkMonitor
import com.zaneschepke.networkmonitor.NetworkMonitor
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.core.tunnel.KernelTunnel
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelProvider
import com.zaneschepke.wireguardautotunnel.core.tunnel.UserspaceTunnel
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.AppSettingRepository
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
    fun provideAmneziaBackend(@ApplicationContext context: Context): Backend {
        return GoBackend(context, RootTunnelActionHandler(org.amnezia.awg.util.RootShell(context)))
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
        serviceManager: ServiceManager,
        appDataRepository: AppDataRepository,
        backend: com.wireguard.android.backend.Backend,
    ): TunnelProvider {
        return KernelTunnel(applicationScope, serviceManager, appDataRepository, backend)
    }

    @Provides
    @Singleton
    @Userspace
    fun provideUserspaceProvider(
        @ApplicationScope applicationScope: CoroutineScope,
        serviceManager: ServiceManager,
        appDataRepository: AppDataRepository,
        backend: Backend,
    ): TunnelProvider {
        return UserspaceTunnel(applicationScope, serviceManager, appDataRepository, backend)
    }

    @Provides
    @Singleton
    fun provideTunnelManager(
        @Kernel kernelTunnel: TunnelProvider,
        @Userspace userspaceTunnel: TunnelProvider,
        appDataRepository: AppDataRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @ApplicationScope applicationScope: CoroutineScope,
    ): TunnelManager {
        return TunnelManager(
            kernelTunnel,
            userspaceTunnel,
            appDataRepository,
            applicationScope,
            ioDispatcher,
        )
    }

    @Provides
    @Singleton
    fun provideNetworkMonitor(
        @ApplicationContext context: Context,
        settingsRepository: AppSettingRepository,
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
                            .map { it.wifiDetectionMethod }

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
        appDataRepository: AppDataRepository,
    ): ServiceManager {
        return ServiceManager(
            context,
            ioDispatcher,
            applicationScope,
            mainCoroutineDispatcher,
            appDataRepository,
        )
    }
}
