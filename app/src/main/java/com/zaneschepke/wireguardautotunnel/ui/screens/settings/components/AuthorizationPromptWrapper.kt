package com.zaneschepke.wireguardautotunnel.ui.screens.settings.components

import androidx.compose.runtime.Composable
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.LocalSharedVm
import com.zaneschepke.wireguardautotunnel.ui.common.prompt.AuthorizationPrompt
import com.zaneschepke.wireguardautotunnel.util.StringValue

@Composable
fun AuthorizationPromptWrapper(onSuccess: () -> Unit, onDismiss: () -> Unit) {
    val sharedViewModel = LocalSharedVm.current
    AuthorizationPrompt(
        onSuccess = { onSuccess() },
        onError = { _ ->
            onDismiss()
            sharedViewModel.showSnackMessage(
                StringValue.StringResource(R.string.error_authentication_failed)
            )
        },
        onFailure = {
            onDismiss()
            sharedViewModel.showSnackMessage(
                StringValue.StringResource(R.string.error_authorization_failed)
            )
        },
    )
}
