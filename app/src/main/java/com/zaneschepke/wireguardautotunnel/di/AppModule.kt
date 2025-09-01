package com.zaneschepke.wireguardautotunnel.di

import android.content.Context
import com.zaneschepke.logcatter.LogReader
import com.zaneschepke.logcatter.LogcatReader
import com.zaneschepke.wireguardautotunnel.core.notification.NotificationManager
import com.zaneschepke.wireguardautotunnel.core.notification.NotificationMonitor
import com.zaneschepke.wireguardautotunnel.core.notification.WireGuardNotification
import com.zaneschepke.wireguardautotunnel.core.shortcut.DynamicShortcutManager
import com.zaneschepke.wireguardautotunnel.core.shortcut.ShortcutManager
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.util.network.NetworkUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Singleton
    @ApplicationScope
    @Provides
    fun providesApplicationScope(
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher
    ): CoroutineScope = CoroutineScope(SupervisorJob() + defaultDispatcher)

    @Singleton
    @Provides
    fun provideLogCollect(@ApplicationContext context: Context): LogReader {
        return LogcatReader.init(storageDir = context.filesDir.absolutePath)
    }

    @Singleton
    @Provides
    fun provideNotificationService(@ApplicationContext context: Context): NotificationManager {
        return WireGuardNotification(context)
    }

    @Singleton
    @Provides
    fun provideShortcutManager(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ShortcutManager {
        return DynamicShortcutManager(context, ioDispatcher)
    }

    @Singleton
    @Provides
    fun provideNetworkUtils(@IoDispatcher ioDispatcher: CoroutineDispatcher): NetworkUtils {
        return NetworkUtils(ioDispatcher)
    }

    @Singleton
    @Provides
    fun provideNotificationMonitor(
        tunnelManager: TunnelManager,
        notificationManager: NotificationManager,
    ): NotificationMonitor {
        return NotificationMonitor(tunnelManager, notificationManager)
    }
}
