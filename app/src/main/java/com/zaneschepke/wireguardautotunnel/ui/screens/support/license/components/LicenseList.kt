package com.zaneschepke.wireguardautotunnel.ui.screens.support.license.components

import LicenseFileEntry
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.ui.common.button.LinkIconButton
import com.zaneschepke.wireguardautotunnel.util.extensions.openWebUrl

@Composable
fun LicenseList(licenses: List<LicenseFileEntry>) {
    val context = LocalContext.current

    LazyColumn(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        items(licenses) { entry ->
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${entry.artifactId} (${entry.version})",
                            style = MaterialTheme.typography.titleSmall,
                        )

                        entry.spdxLicenses.forEach { license ->
                            Text(
                                text = license.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    entry.scm?.url?.let { scmUrl ->
                        LinkIconButton(modifier = Modifier.size(20.dp).focusable()) {
                            context.openWebUrl(scmUrl)
                        }
                    }
                }
            }
        }
    }
}
