package com.zaneschepke.wireguardautotunnel.util

import com.wireguard.android.util.RootShell
import com.zaneschepke.wireguardautotunnel.di.AppShell
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import javax.inject.Provider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class RootShellUtils(
    @AppShell private val rootShell: Provider<RootShell>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend fun requestRoot(): Boolean =
        withContext(ioDispatcher) {
            val accepted =
                try {
                    rootShell.get().start()
                    true
                } catch (_: Exception) {
                    false
                }
            accepted
        }
}
