package com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance.language

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.LocalSharedVm
import com.zaneschepke.wireguardautotunnel.ui.common.button.IconSurfaceButton
import com.zaneschepke.wireguardautotunnel.util.LocaleUtil
import java.text.Collator
import java.util.*

@Composable
fun LanguageScreen() {

    val sharedViewModel = LocalSharedVm.current
    val appState by sharedViewModel.container.stateFlow.collectAsStateWithLifecycle()

    val collator = Collator.getInstance(Locale.getDefault())
    val locales =
        LocaleUtil.supportedLocales.map {
            val tag = it.replace("_", "-")
            Locale.forLanguageTag(tag)
        }

    val sortedLocales =
        remember(locales) {
            locales.sortedWith(compareBy(collator) { it.getDisplayName(it) }).toList()
        }

    val lazyListState = rememberLazyListState()

    val selectedIndex =
        remember(appState.locale, sortedLocales) {
            if (appState.locale == LocaleUtil.OPTION_PHONE_LANGUAGE) 0
            else {
                val selectedLocale = Locale.forLanguageTag(appState.locale)
                sortedLocales.indexOfFirst {
                    it.toLanguageTag() == selectedLocale.toLanguageTag()
                } + 1
            }
        }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) {
            lazyListState.scrollToItem(selectedIndex)
        }
    }

    LazyColumn(
        state = lazyListState,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier.padding(horizontal = 12.dp),
    ) {
        item {
            IconSurfaceButton(
                stringResource(R.string.automatic),
                { sharedViewModel.setLocale(LocaleUtil.OPTION_PHONE_LANGUAGE) },
                selected = appState.locale == LocaleUtil.OPTION_PHONE_LANGUAGE,
            )
        }
        items(sortedLocales, key = { it }) { locale ->
            val buttonText =
                locale.getDisplayName(locale).replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(locale) else it.toString()
                }
            IconSurfaceButton(
                buttonText,
                onClick = { sharedViewModel.setLocale(locale.toLanguageTag()) },
                selected = appState.locale == locale.toLanguageTag(),
            )
        }
    }
}
