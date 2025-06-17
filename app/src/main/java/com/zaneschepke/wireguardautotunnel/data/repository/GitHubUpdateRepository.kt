package com.zaneschepke.wireguardautotunnel.data.repository

import android.content.Context
import com.zaneschepke.wireguardautotunnel.BuildConfig
import com.zaneschepke.wireguardautotunnel.data.mapper.GitHubReleaseMapper
import com.zaneschepke.wireguardautotunnel.data.network.GitHubApi
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.model.AppUpdate
import com.zaneschepke.wireguardautotunnel.domain.repository.UpdateRepository
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.NumberUtils
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber

class GitHubUpdateRepository(
    private val gitHubApi: GitHubApi,
    private val httpClient: HttpClient,
    private val githubOwner: String,
    private val githubRepo: String,
    private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UpdateRepository {
    override suspend fun checkForUpdate(currentVersion: String): Result<AppUpdate?> =
        withContext(ioDispatcher) {
            Timber.i("Checking for update")
            val isNightly = BuildConfig.VERSION_NAME.contains("nightly")
            val release =
                if (isNightly) {
                    gitHubApi.getNightlyRelease(githubOwner, githubRepo).onFailure(Timber::e)
                } else {
                    gitHubApi.getLatestRelease(githubOwner, githubRepo).onFailure(Timber::e)
                }
            release.map { release ->
                val apkAsset =
                    release.assets.find { asset ->
                        asset.name.startsWith("wgtunnel-${Constants.STANDALONE_FLAVOR}-v") &&
                            asset.name.endsWith(".apk")
                    }
                val newVersion =
                    apkAsset
                        ?.name
                        ?.removePrefix("wgtunnel-${Constants.STANDALONE_FLAVOR}-v")
                        ?.removeSuffix(".apk") ?: return@map null

                Timber.i("Latest version: $newVersion, current version: $currentVersion")
                if (isNightly && newVersion != currentVersion)
                    return@map GitHubReleaseMapper.toAppUpdate(release, newVersion)
                if (NumberUtils.compareVersions(newVersion, currentVersion) > 0) {
                    GitHubReleaseMapper.toAppUpdate(release, newVersion)
                } else {
                    null
                }
            }
        }

    override suspend fun downloadApk(
        apkUrl: String,
        fileName: String,
        onProgress: (Float) -> Unit,
    ): Result<File> =
        withContext(ioDispatcher) {
            try {
                // clean up old files
                context.getExternalFilesDir(null)?.listFiles()?.forEach { file ->
                    if (file.extension == "apk") file.delete()
                }

                val response: HttpResponse = httpClient.get(apkUrl)

                val apkFile = File(context.getExternalFilesDir(null), fileName)

                val channel: ByteReadChannel = response.bodyAsChannel()
                val totalBytes: Long = response.contentLength() ?: -1L
                var bytesCopied = 0L

                apkFile.outputStream().use { output ->
                    val buffer = ByteArray(8 * 1024)

                    while (!channel.isClosedForRead) {
                        val bytesRead = channel.readAvailable(buffer)
                        if (bytesRead <= 0) break
                        output.write(buffer, 0, bytesRead)
                        bytesCopied += bytesRead

                        if (totalBytes > 0) {
                            val progress = bytesCopied.toFloat() / totalBytes
                            onProgress(progress.coerceIn(0f, 1f))
                        }
                    }
                }

                Result.success(apkFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
