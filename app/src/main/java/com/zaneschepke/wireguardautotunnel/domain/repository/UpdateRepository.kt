package com.zaneschepke.wireguardautotunnel.domain.repository

import com.zaneschepke.wireguardautotunnel.domain.model.AppUpdate
import java.io.File

interface UpdateRepository {
    suspend fun checkForUpdate(currentVersion: String): Result<AppUpdate?>

    suspend fun downloadApk(
        apkUrl: String,
        fileName: String,
        onProgress: suspend (Float) -> Unit,
    ): Result<File>
}
