package com.zaneschepke.wireguardautotunnel.ui.common

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.zaneschepke.wireguardautotunnel.MainActivity

@Composable
fun SecureScreenFromRecording() {
    val context = LocalContext.current

    val activity = context as? MainActivity

    // Secure screen due to sensitive information
    DisposableEffect(Unit) {
        activity
            ?.window
            ?.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}