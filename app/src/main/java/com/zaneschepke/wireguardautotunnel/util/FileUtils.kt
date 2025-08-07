package com.zaneschepke.wireguardautotunnel.util

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.entity.TunnelConfig
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.util.extensions.getInputStreamFromUri
import com.zaneschepke.wireguardautotunnel.util.extensions.installApk
import com.zaneschepke.wireguardautotunnel.util.extensions.launchShareFile
import com.zaneschepke.wireguardautotunnel.util.extensions.toWgQuickString
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.amnezia.awg.config.Config
import timber.log.Timber

class FileUtils(private val context: Context, private val ioDispatcher: CoroutineDispatcher) {

    suspend fun createWgFiles(tunnels: List<TunnelConf>): List<File> =
        withContext(ioDispatcher) {
            tunnels.mapNotNull { config ->
                if (config.wgQuick.isBlank()) {
                    Timber.w("Skipping tunnel ${config.tunName}: empty wgQuick config")
                    return@mapNotNull null
                }
                val file = File(context.cacheDir, "${config.tunName}-wg.conf")
                file.outputStream().use { it.write(config.wgQuick.toByteArray()) }
                Timber.d("Created WG file: ${file.path}, size: ${file.length()} bytes")
                if (file.length() == 0L) {
                    Timber.w("WG file ${file.path} is empty")
                    null
                } else {
                    file
                }
            }
        }

    suspend fun createAmFiles(tunnels: List<TunnelConf>): List<File> =
        withContext(ioDispatcher) {
            tunnels
                .filter { it.amQuick != TunnelConfig.AM_QUICK_DEFAULT && it.amQuick.isNotBlank() }
                .mapNotNull { config ->
                    val file = File(context.cacheDir, "${config.tunName}-am.conf")
                    file.outputStream().use { it.write(config.amQuick.toByteArray()) }
                    Timber.d("Created AM file: ${file.path}, size: ${file.length()} bytes")
                    if (file.length() == 0L) {
                        Timber.w("AM file ${file.path} is empty")
                        null
                    } else {
                        file
                    }
                }
        }

    suspend fun zipAll(zipFile: File, files: List<File>) =
        withContext(ioDispatcher) {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                files.forEach { file ->
                    if (!file.exists() || file.length() == 0L) {
                        Timber.w("Skipping file ${file.path}: does not exist or empty")
                        return@forEach
                    }
                    val entryName = file.name // Use file name only, avoid complex path logic
                    val entry = ZipEntry(entryName)
                    zos.putNextEntry(entry)
                    FileInputStream(file).use { fis ->
                        fis.copyTo(zos)
                        Timber.d(
                            "Added ${file.path} to zip as $entryName, size: ${file.length()} bytes"
                        )
                    }
                    zos.closeEntry()
                }
                zos.flush()
                Timber.d("Finished zipping: ${zipFile.path}, size: ${zipFile.length()} bytes")
            }
        }

    suspend fun createNewShareFile(name: String): File =
        withContext(ioDispatcher) {
            val sharePath = File(context.filesDir, "external_files")
            if (sharePath.exists()) sharePath.deleteRecursively()
            sharePath.mkdirs()
            val file = File(sharePath, name)
            if (file.exists()) file.delete()
            file.createNewFile()
            Timber.d("Created share file: ${file.path}")
            file
        }

    suspend fun copyFileToUri(sourceFile: File, destinationUri: Uri): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                if (!sourceFile.exists()) {
                    Timber.e("Source file does not exist: ${sourceFile.path}")
                    return@withContext Result.failure(IOException("Source file does not exist"))
                }
                if (!sourceFile.canRead()) {
                    Timber.e("Source file is not readable: ${sourceFile.path}")
                    return@withContext Result.failure(IOException("Source file is not readable"))
                }
                if (sourceFile.length() == 0L) {
                    Timber.e("Source file is empty: ${sourceFile.path}")
                    return@withContext Result.failure(IOException("Source file is empty"))
                }

                Timber.d("Copying file: ${sourceFile.path}, size: ${sourceFile.length()} bytes")
                var bytesCopied = 0L
                FileInputStream(sourceFile).use { inputStream ->
                    context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                        val buffer = ByteArray(1024 * 1024) // 1MB buffer
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            bytesCopied += bytesRead
                            Timber.d("Copied $bytesCopied bytes so far")
                        }
                        outputStream.flush()
                        Timber.d("Total bytes copied: $bytesCopied")
                        Result.success(Unit)
                    }
                        ?: run {
                            Timber.e("Failed to open OutputStream for Uri: $destinationUri")
                            Result.failure(IOException("Failed to open OutputStream for Uri"))
                        }
                }
            } catch (e: IOException) {
                Timber.e(e, "Error copying file to Uri: ${e.message}")
                Result.failure(e)
            }
        }

    private fun getDisplayNameColumnIndex(cursor: Cursor): Int? {
        val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (columnIndex == -1) return null
        return columnIndex
    }

    private fun getDisplayNameByCursor(cursor: Cursor): String? {
        val move = cursor.moveToFirst()
        if (!move) return null
        val index = getDisplayNameColumnIndex(cursor)
        if (index == null) return index
        return cursor.getString(index)
    }

    private fun isValidUriContentScheme(uri: Uri): Boolean {
        return uri.scheme == Constants.URI_CONTENT_SCHEME
    }

    private fun getFileName(uri: Uri): String {
        return getFileNameByCursor(uri) ?: NumberUtils.generateRandomTunnelName()
    }

    private fun getNameFromFileName(fileName: String): String {
        return fileName.substring(0, fileName.lastIndexOf('.'))
    }

    private fun getFileExtensionFromFileName(fileName: String): String? {
        return try {
            fileName.substring(fileName.lastIndexOf('.'))
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }

    private fun getFileNameByCursor(uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use {
            getDisplayNameByCursor(it)
        }
    }

    suspend fun buildTunnelsFromUri(uri: Uri): List<TunnelConf> =
        withContext(ioDispatcher) {
            if (!isValidUriContentScheme(uri)) throw InvalidFileExtensionException
            val fileName = getFileName(uri)
            when (getFileExtensionFromFileName(fileName)) {
                Constants.CONF_FILE_EXTENSION -> {
                    context.getInputStreamFromUri(uri)?.use { inputStream ->
                        val name = getNameFromFileName(fileName)
                        val amConf = Config.parse(inputStream)
                        listOf(
                            TunnelConf(
                                tunName = name,
                                wgQuick = amConf.toWgQuickString(),
                                amQuick = amConf.toAwgQuickString(true),
                            )
                        )
                    } ?: throw FileReadException
                }
                Constants.ZIP_FILE_EXTENSION -> {
                    ZipInputStream(context.getInputStreamFromUri(uri)).use { zip ->
                        generateSequence { zip.nextEntry }
                            .filterNot {
                                it.isDirectory ||
                                    getFileExtensionFromFileName(it.name) !=
                                        Constants.CONF_FILE_EXTENSION
                            }
                            .map { entry ->
                                val name = getNameFromFileName(entry.name)
                                val amConf = Config.parse(zip.bufferedReader())
                                TunnelConf(
                                    tunName = name,
                                    wgQuick = amConf.toWgQuickString(),
                                    amQuick = amConf.toAwgQuickString(true),
                                )
                            }
                            .toList()
                    }
                }
                else -> throw InvalidFileExtensionException
            }
        }

    fun shareFile(shareFile: File) {
        val uri =
            FileProvider.getUriForFile(context, context.getString(R.string.provider), shareFile)
        context.launchShareFile(uri)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun saveToDownloadsWithMediaStore(file: File, mimeType: String): Uri? =
        withContext(ioDispatcher) {
            val contentValues =
                ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }

            val resolver = context.contentResolver
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val itemUri = resolver.insert(collection, contentValues) ?: return@withContext null

            resolver.openOutputStream(itemUri)?.use { output ->
                file.inputStream().use { input -> input.copyTo(output) }
            }
            // Mark as finished
            contentValues.clear()
            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(itemUri, contentValues, null, null)

            return@withContext itemUri
        }

    fun installApk(file: File) {
        context.installApk(file)
    }
}
