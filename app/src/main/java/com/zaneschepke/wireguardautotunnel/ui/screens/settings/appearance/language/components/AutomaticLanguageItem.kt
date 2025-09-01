package com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance.language.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.LocalIsAndroidTV
import com.zaneschepke.wireguardautotunnel.ui.common.button.SelectionItemButton
import com.zaneschepke.wireguardautotunnel.ui.common.label.SelectedLabel
import com.zaneschepke.wireguardautotunnel.util.LocaleUtil

@Composable
fun automaticLanguageItem(locale: String, onChange: (locale: String) -> Unit) {
    val isTv = LocalIsAndroidTV.current
    Box(modifier = Modifier.padding(top = 24.dp)) {
        SelectionItemButton(
            buttonText = stringResource(R.string.automatic),
            onClick = { onChange(LocaleUtil.OPTION_PHONE_LANGUAGE) },
            trailing = {
                if (locale == LocaleUtil.OPTION_PHONE_LANGUAGE) {
                    SelectedLabel()
                }
            },
            ripple = isTv,
        )
    }
}
