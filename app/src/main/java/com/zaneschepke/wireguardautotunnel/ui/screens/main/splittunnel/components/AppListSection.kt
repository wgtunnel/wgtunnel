package com.zaneschepke.wireguardautotunnel.ui.screens.main.splittunnel.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.textbox.CustomTextField
import com.zaneschepke.wireguardautotunnel.ui.screens.main.splittunnel.state.TunnelApp

@Composable
fun AppListSection(
    apps: List<Pair<TunnelApp, Boolean>>,
    onAppSelectionToggle: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    query: String,
) {
    val inputHeight = 45.dp

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        CustomTextField(
            textStyle =
                MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ),
            value = query,
            onValueChange = onQueryChange,
            interactionSource = remember { MutableInteractionSource() },
            label = {},
            leading = { Icon(Icons.Outlined.Search, stringResource(R.string.search)) },
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().height(inputHeight).padding(horizontal = 24.dp),
            singleLine = true,
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Done,
                ),
            keyboardActions = KeyboardActions(),
        )
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            contentPadding = PaddingValues(top = 10.dp),
        ) {
            items(apps, key = { it.first.`package` }) { app ->
                AppListItem(
                    appInfo = app.first,
                    isSelected = app.second,
                    onToggle = { onAppSelectionToggle(app.first.`package`) },
                )
            }
        }
    }
}
