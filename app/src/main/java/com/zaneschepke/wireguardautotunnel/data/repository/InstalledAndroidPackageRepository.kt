package com.zaneschepke.wireguardautotunnel.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.model.InstalledPackage
import com.zaneschepke.wireguardautotunnel.domain.repository.InstalledPackageRepository
import com.zaneschepke.wireguardautotunnel.util.extensions.getAllInternetCapablePackages
import com.zaneschepke.wireguardautotunnel.util.extensions.getFriendlyAppName
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class InstalledAndroidPackageRepository(
    private val context: Context,
    @ApplicationScope val applicationScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : InstalledPackageRepository {

    private var cachedPackages: List<InstalledPackage>? = null

    init {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    when (intent.action) {
                        Intent.ACTION_PACKAGE_ADDED,
                        Intent.ACTION_PACKAGE_REMOVED,
                        Intent.ACTION_PACKAGE_CHANGED -> {
                            // don't update if we have nothing cached
                            if (cachedPackages == null) return
                            Timber.d("Updating installed packages cache")
                            applicationScope.launch { refreshInstalledPackages() }
                        }
                    }
                }
            }
        val filter =
            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addDataScheme("package")
            }
        context.registerReceiver(receiver, filter)
    }

    override suspend fun getInstalledPackages(): List<InstalledPackage> =
        withContext(ioDispatcher) {
            cachedPackages?.let {
                return@withContext it
            }
            refreshInstalledPackages()
        }

    override suspend fun refreshInstalledPackages(): List<InstalledPackage> =
        withContext(ioDispatcher) {
            val packages = context.getAllInternetCapablePackages()

            val installedPackages =
                packages.mapNotNull { packageInfo ->
                    try {
                        val appInfo =
                            context.packageManager.getApplicationInfo(packageInfo.packageName, 0)
                        InstalledPackage(
                            name =
                                context.packageManager.getFriendlyAppName(
                                    packageInfo.packageName,
                                    appInfo,
                                ),
                            packageName = packageInfo.packageName,
                            uId = appInfo.uid,
                        )
                    } catch (e: PackageManager.NameNotFoundException) {
                        Timber.e(e)
                        null
                    }
                }

            cachedPackages = installedPackages

            installedPackages
        }
}
