package com.zaneschepke.wireguardautotunnel.ui.screens.support.license

import LicenseFileEntry
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.LocalSharedVm
import com.zaneschepke.wireguardautotunnel.ui.screens.support.license.components.LicenseList
import com.zaneschepke.wireguardautotunnel.ui.state.NavbarState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@Composable
fun LicenseScreen() {
    val context = LocalContext.current
    val sharedViewModel = LocalSharedVm.current
    var licenses by remember { mutableStateOf<List<LicenseFileEntry>>(emptyList()) }

    LaunchedEffect(Unit) {
        sharedViewModel.updateNavbarState(
            NavbarState(
                showTopItems = true,
                showBottomItems = true,
                topTitle = { Text(stringResource(R.string.licenses)) },
            )
        )
    }

    LaunchedEffect(Unit) { licenses = loadLicenseeJson(context) }

    if (licenses.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
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
