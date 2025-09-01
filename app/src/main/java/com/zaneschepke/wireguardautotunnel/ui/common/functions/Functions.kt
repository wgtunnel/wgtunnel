package com.zaneschepke.wireguardautotunnel.ui.common.functions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.zaneschepke.wireguardautotunnel.ui.LocalIsAndroidTV
import com.zaneschepke.wireguardautotunnel.util.FileUtils
import timber.log.Timber

@Composable
fun rememberFileImportLauncherForResult(
    onNoFileExplorer: () -> Unit,
    onData: (data: Uri) -> Unit,
): ManagedActivityResultLauncher<String, Uri?> {
    val isTv = LocalIsAndroidTV.current
    return rememberLauncherForActivityResult(
        object : ActivityResultContracts.GetContent() {
            override fun createIntent(context: Context, input: String): Intent {
                val intent =
                    super.createIntent(context, input).apply {
                        type =
                            if (isTv) {
                                FileUtils.ALLOWED_TV_FILE_TYPES
                            } else {
                                FileUtils.ALL_FILE_TYPES
                            }
                    }

                /* AndroidTV now comes with stubs that do nothing but display a Toast less helpful than
                 * what we can do, so detect this and throw an exception that we can catch later. */
                val activitiesToResolveIntent =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.packageManager.queryIntentActivities(
                            intent,
                            PackageManager.ResolveInfoFlags.of(
                                PackageManager.MATCH_DEFAULT_ONLY.toLong()
                            ),
                        )
                    } else {
                        context.packageManager.queryIntentActivities(
                            intent,
                            PackageManager.MATCH_DEFAULT_ONLY,
                        )
                    }
                if (
                    activitiesToResolveIntent.all {
                        val name = it.activityInfo.packageName
                        name.startsWith(FileUtils.GOOGLE_TV_EXPLORER_STUB) ||
                            name.startsWith(FileUtils.ANDROID_TV_EXPLORER_STUB)
                    }
                ) {
                    onNoFileExplorer()
                }
                return intent
            }
        }
    ) { data ->
        if (data == null) return@rememberLauncherForActivityResult
        onData(data)
    }
}

@Composable
fun rememberFileExportLauncherForResult(
    mimeType: String = FileUtils.ZIP_FILE_MIME_TYPE,
    onResult: (Uri?) -> Unit,
): ManagedActivityResultLauncher<String, Uri?> {
    val isTv = LocalIsAndroidTV.current
    return rememberLauncherForActivityResult(
        contract =
            object : ActivityResultContracts.CreateDocument(mimeType) {
                override fun createIntent(context: Context, input: String): Intent {
                    super.createIntent(context, input)
                    val intent =
                        Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type =
                                if (isTv) {
                                    FileUtils.ALLOWED_TV_FILE_TYPES
                                } else {
                                    mimeType
                                }
                            putExtra(Intent.EXTRA_TITLE, input)
                        }

                    Timber.d("Returning SAF intent for launch")
                    return intent
                }
            }
    ) { uri ->
        Timber.d("SAF onResult called with Uri: $uri")
        if (uri != null) {
            Timber.d(
                "Uri details: scheme=${uri.scheme}, authority=${uri.authority}, path=${uri.path}"
            )
        } else {
            Timber.d("SAF picker canceled or failed to return a Uri")
        }
        onResult(uri)
    }
}
