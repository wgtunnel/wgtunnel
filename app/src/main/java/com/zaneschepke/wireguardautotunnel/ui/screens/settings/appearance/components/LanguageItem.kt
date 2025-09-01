package com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ForwardButton
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.navigation.Route

@Composable
fun LanguageItem(navController: NavController): SelectionItem {
    return SelectionItem(
        leading = { Icon(Icons.Outlined.Translate, contentDescription = null) },
        title = {
            Text(
                text = stringResource(R.string.language),
                style =
                    MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
            )
        },
        trailing = { ForwardButton { navController.navigate(Route.Language) } },
        onClick = { navController.navigate(Route.Language) },
    )
}
