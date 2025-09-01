package com.zaneschepke.wireguardautotunnel.domain.repository

import com.zaneschepke.wireguardautotunnel.domain.model.InstalledPackage

interface InstalledPackageRepository {

    // gets packages from cache or queries and updates cache if empty
    suspend fun getInstalledPackages(): List<InstalledPackage>

    // updates the cache and returns the results
    suspend fun refreshInstalledPackages(): List<InstalledPackage>
}
