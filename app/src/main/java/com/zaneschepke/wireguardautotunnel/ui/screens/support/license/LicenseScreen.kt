package com.zaneschepke.wireguardautotunnel.ui.screens.support.license

import LicenseFileEntry
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.ui.screens.support.license.components.LicenseList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LicenseScreen() {
    val context = LocalContext.current
    var licenses by remember { mutableStateOf<List<LicenseFileEntry>>(emptyList()) }

    LaunchedEffect(Unit) { licenses = loadLicenseeJson(context) }

    if (licenses.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularWavyProgressIndicator(waveSpeed = 60.dp, modifier = Modifier.size(48.dp))
        }
    } else {
        LicenseList(licenses)
    }
}

suspend fun loadLicenseeJson(context: Context): List<LicenseFileEntry> {
    return withContext(Dispatchers.IO) {
        val json = Json { ignoreUnknownKeys = true }

        val jsonResult = context.assets.open("licenses.json").bufferedReader().use { it.readText() }
        json.decodeFromString(jsonResult)
    }
}
