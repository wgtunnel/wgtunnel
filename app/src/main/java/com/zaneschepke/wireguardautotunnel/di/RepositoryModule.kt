package com.zaneschepke.wireguardautotunnel.di

import android.content.Context
import androidx.room.Room
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.AppDatabase
import com.zaneschepke.wireguardautotunnel.data.DataStoreManager
import com.zaneschepke.wireguardautotunnel.data.DatabaseCallback
import com.zaneschepke.wireguardautotunnel.data.dao.*
import com.zaneschepke.wireguardautotunnel.data.migrations.MIGRATION_23_24
import com.zaneschepke.wireguardautotunnel.data.network.GitHubApi
import com.zaneschepke.wireguardautotunnel.data.network.KtorClient
import com.zaneschepke.wireguardautotunnel.data.network.KtorGitHubApi
import com.zaneschepke.wireguardautotunnel.data.repository.*
import com.zaneschepke.wireguardautotunnel.domain.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.*
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

@Module
@InstallIn(SingletonComponent::class)
class RepositoryModule {

    @Provides
    @Singleton
    fun provideGlobalEffectRepository(): GlobalEffectRepository {
        return GlobalEffectRepository()
    }

    @Provides
    @Singleton
    fun provideInstalledPackageRepository(
        @ApplicationContext context: Context,
        @ApplicationScope applicationScope: CoroutineScope,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): InstalledPackageRepository {
        return InstalledAndroidPackageRepository(context, applicationScope, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        callback: DatabaseCallback,
        dataStoreManager: DataStoreManager,
    ): AppDatabase {
        return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                context.getString(R.string.db_name),
            )
            .addMigrations(MIGRATION_23_24(dataStoreManager.dataStore))
            .fallbackToDestructiveMigration(true)
            .addCallback(callback)
            .build()
    }

    @Singleton
    @Provides
    fun provideSettingsDoa(appDatabase: AppDatabase): GeneralSettingsDao {
        return appDatabase.generalSettingsDao()
    }

    @Singleton
    @Provides
    fun provideDnsSettingsDao(appDatabase: AppDatabase): DnsSettingsDao {
        return appDatabase.dnsSettingsDao()
    }

    @Singleton
    @Provides
    fun provideAutoTunnelDao(appDatabase: AppDatabase): AutoTunnelSettingsDao {
        return appDatabase.autoTunnelSettingsDao()
    }

    @Singleton
    @Provides
    fun provideMonitoringDao(appDatabase: AppDatabase): MonitoringSettingsDao {
        return appDatabase.monitoringSettingsDao()
    }

    @Singleton
    @Provides
    fun provideProxyDoa(appDatabase: AppDatabase): ProxySettingsDao {
        return appDatabase.proxySettingsDoa()
    }

    @Singleton
    @Provides
    fun provideTunnelConfigDoa(appDatabase: AppDatabase): TunnelConfigDao {
        return appDatabase.tunnelConfigDoa()
    }

    @Singleton
    @Provides
    fun provideTunnelConfigRepository(
        tunnelConfigDao: TunnelConfigDao,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): TunnelRepository {
        return RoomTunnelRepository(tunnelConfigDao, ioDispatcher)
    }

    @Singleton
    @Provides
    fun provideGeneralSettingsRepository(
        settingsDao: GeneralSettingsDao,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): GeneralSettingRepository {
        return RoomSettingsRepository(settingsDao, ioDispatcher)
    }

    @Singleton
    @Provides
    fun provideMonitoringSettingsRepository(
        monitoringSettingsDao: MonitoringSettingsDao,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): MonitoringSettingsRepository {
        return RoomMonitoringSettingsRepository(monitoringSettingsDao, ioDispatcher)
    }

    @Singleton
    @Provides
    fun provideDnsSettingsRepository(
        dnsSettingsDao: DnsSettingsDao,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): DnsSettingsRepository {
        return RoomDnsSettingsRepository(dnsSettingsDao, ioDispatcher)
    }

    @Singleton
    @Provides
    fun provideAutoTunnelSettingsRepository(
        autoTunnelSettingsDao: AutoTunnelSettingsDao,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): AutoTunnelSettingsRepository {
        return RoomAutoTunnelSettingsRepository(autoTunnelSettingsDao, ioDispatcher)
    }

    @Singleton
    @Provides
    fun provideProxySettingsRepository(
        proxySettingsDao: ProxySettingsDao,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ProxySettingsRepository {
        return RoomProxySettingsRepository(proxySettingsDao, ioDispatcher)
    }

    @Singleton
    @Provides
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): DataStoreManager {
        return DataStoreManager(context, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideGeneralStateRepository(
        dataStoreManager: DataStoreManager,
        @ApplicationScope applicationScope: CoroutineScope,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): AppStateRepository {
        return DataStoreAppStateRepository(dataStoreManager, applicationScope, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return KtorClient.create()
    }

    @Provides
    @Singleton
    fun provideGitHubApi(client: HttpClient): GitHubApi {
        return KtorGitHubApi(client)
    }

    @Provides
    @Singleton
    fun provideUpdateRepository(
        gitHubApi: GitHubApi,
        client: HttpClient,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @ApplicationContext context: Context,
    ): UpdateRepository {
        return GitHubUpdateRepository(
            gitHubApi,
            client,
            "wgtunnel",
            "wgtunnel",
            context,
            ioDispatcher,
        )
    }
}
