package com.zaneschepke.wireguardautotunnel.di

import android.content.Context
import com.wireguard.android.util.RootShell
import com.zaneschepke.wireguardautotunnel.util.FileUtils
import com.zaneschepke.wireguardautotunnel.util.RootShellUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import io.ktor.utils.io.ioDispatcher
import javax.inject.Provider
import kotlinx.coroutines.CoroutineDispatcher

@Module
@InstallIn(ViewModelComponent::class)
class ViewModelModule {
    @ViewModelScoped
    @Provides
    fun provideFileUtils(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): FileUtils {
        return FileUtils(context, ioDispatcher)
    }

    @ViewModelScoped
    @Provides
    fun provideRootShellUtils(
        @AppShell rootShell: Provider<RootShell>,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): RootShellUtils {
        return RootShellUtils(rootShell, ioDispatcher)
    }
}
