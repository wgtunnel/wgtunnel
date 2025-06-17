package com.zaneschepke.wireguardautotunnel.ui.screens.support

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.BuildConfig
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.SectionDivider
import com.zaneschepke.wireguardautotunnel.ui.common.dialog.InfoDialog
import com.zaneschepke.wireguardautotunnel.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.ui.screens.support.components.ContactSupportOptions
import com.zaneschepke.wireguardautotunnel.ui.screens.support.components.GeneralSupportOptions
import com.zaneschepke.wireguardautotunnel.ui.screens.support.components.UpdateSection
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.canInstallPackages
import com.zaneschepke.wireguardautotunnel.util.extensions.openWebUrl
import com.zaneschepke.wireguardautotunnel.util.extensions.requestInstallPackagesPermission
import com.zaneschepke.wireguardautotunnel.util.extensions.showToast
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun SupportScreen(viewModel: SupportViewModel = hiltViewModel(), appViewModel: AppViewModel) {
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showPermissionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            viewModel.handleErrorShown()
            appViewModel.handleEvent(AppEvent.ShowMessage(it))
            viewModel.handleErrorShown()
        }
    }

    LaunchedEffect(uiState.isUptoDate) {
        if (uiState.isUptoDate == true)
            return@LaunchedEffect context.showToast(R.string.latest_installed)
    }

    if (uiState.appUpdate != null) {
        InfoDialog(
            onDismiss = { viewModel.handleUpdateShown() },
            onAttest = {
                if (BuildConfig.FLAVOR != Constants.STANDALONE_FLAVOR) {
                    uiState.appUpdate?.apkUrl?.let { context.openWebUrl(it) }
                    return@InfoDialog
                }
                if (context.canInstallPackages()) {
                    viewModel.handleDownloadAndInstallApk()
                } else {
                    showPermissionDialog = true
                }
            },
            title = { Text(stringResource(R.string.update_available)) },
            body = {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val annotatedString = buildAnnotatedString {
                        append("${uiState.appUpdate?.version ?: ""}\n")
                        // Add clickable text for second line
                        withLink(
                            link =
                                LinkAnnotation.Clickable(
                                    tag = stringResource(id = R.string.release_notes),
                                    linkInteractionListener = {
                                        val version =
                                            if (BuildConfig.VERSION_NAME.contains("nightly")) {
                                                "nightly"
                                            } else {
                                                uiState.appUpdate
                                                    ?.version
                                                    ?.removePrefix("v")
                                                    ?.trim() ?: ""
                                            }
                                        val url = "${Constants.BASE_RELEASE_URL}$version".trim()
                                        context.openWebUrl(url)
                                    },
                                    styles =
                                        TextLinkStyles(
                                            style =
                                                SpanStyle(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    textDecoration = TextDecoration.Underline,
                                                )
                                        ),
                                )
                        ) {
                            append(stringResource(R.string.release_notes))
                        }
                    }

                    Text(text = annotatedString)
                    if (uiState.isLoading) {
                        LinearProgressIndicator(
                            progress = { uiState.downloadProgress },
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            color = MaterialTheme.colorScheme.primary,
                            strokeCap = StrokeCap.Round,
                        )
                    }
                }
            },
            confirmText = {
                Text(
                    if (BuildConfig.FLAVOR != Constants.STANDALONE_FLAVOR)
                        stringResource(R.string.download)
                    else stringResource(R.string.download_and_install)
                )
            },
        )
    }

    if (showPermissionDialog) {
        InfoDialog(
            onDismiss = { showPermissionDialog = false },
            onAttest = {
                context.requestInstallPackagesPermission()
                showPermissionDialog = false
            },
            title = { Text(stringResource(R.string.permission_required)) },
            body = { Text(stringResource(R.string.install_updated_permission)) },
            confirmText = { Text(stringResource(R.string.allow)) },
        )
    }

    Column(
        modifier =
            Modifier.fillMaxSize()
                .padding(vertical = 24.dp)
                .padding(horizontal = 12.dp)
                .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
    ) {
        GroupLabel(
            stringResource(R.string.thank_you),
            modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
        )
        UpdateSection(
            onUpdateCheck = {
                if (
                    BuildConfig.DEBUG ||
                        BuildConfig.VERSION_NAME.contains("beta") ||
                        BuildConfig.FLAVOR == Constants.GOOGLE_PLAY_FLAVOR
                )
                    return@UpdateSection context.showToast(R.string.update_check_unsupported)
                context.showToast(R.string.checking_for_update)
                viewModel.handleUpdateCheck()
            }
        )
        SectionDivider()
        GeneralSupportOptions(context)
        SectionDivider()
        ContactSupportOptions(context)
    }
}
