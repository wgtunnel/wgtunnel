package com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance.language.components

import androidx.compose.runtime.Composable
import com.zaneschepke.wireguardautotunnel.ui.LocalIsAndroidTV
import com.zaneschepke.wireguardautotunnel.ui.common.button.SelectionItemButton
import com.zaneschepke.wireguardautotunnel.ui.common.label.SelectedLabel
import java.util.*

@Composable
fun languageItem(currentLocale: String, locale: Locale, onChange: (String) -> Unit) {
    val isTv = LocalIsAndroidTV.current
    SelectionItemButton(
        buttonText =
            locale.getDisplayLanguage(locale).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(locale) else it.toString()
            } +
                if (locale.toLanguageTag().contains("-")) {
                    " (${locale.getDisplayCountry(locale).replaceFirstChar {
				if (it.isLowerCase()) it.titlecase(locale) else it.toString()
			}})"
                } else {
                    ""
                },
        onClick = { onChange(locale.toLanguageTag()) },
        trailing = {
            if (locale.toLanguageTag() == currentLocale) {
                SelectedLabel()
            }
        },
        ripple = isTv,
    )
}
