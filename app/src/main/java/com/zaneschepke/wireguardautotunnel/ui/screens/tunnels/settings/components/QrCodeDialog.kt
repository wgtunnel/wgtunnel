package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.settings.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.zaneschepke.wireguardautotunnel.MainActivity
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.enums.ConfigType
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.util.extensions.isTextTooLargeForQr
import com.zaneschepke.wireguardautotunnel.util.extensions.setScreenBrightness
import com.zaneschepke.wireguardautotunnel.util.extensions.showToast
import io.github.alexzhirkevich.qrose.options.*
import io.github.alexzhirkevich.qrose.rememberQrCodePainter

@Composable
fun QrCodeDialog(tunnelConfig: TunnelConfig, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? MainActivity

    // Handle screen brightness
    DisposableEffect(Unit) {
        activity?.setScreenBrightness(1.0f)
        onDispose { activity?.setScreenBrightness(-1f) }
    }

    QrCodeAlertDialog(tunnelConfig = tunnelConfig, onDismiss = onDismiss)
}

@Composable
private fun QrCodeAlertDialog(tunnelConfig: TunnelConfig, onDismiss: () -> Unit) {
    AlertDialog(
        containerColor = Color.White,
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.done), color = MaterialTheme.colorScheme.surface)
            }
        },
        title = {
            Text(
                text = tunnelConfig.name,
                color = Color.Black,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = { QrCodeContent(tunnelConfig = tunnelConfig, onDismiss) },
        properties = DialogProperties(usePlatformDefaultWidth = true),
    )
}

@Composable
private fun QrCodeContent(tunnelConfig: TunnelConfig, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var selectedOption by remember { mutableStateOf(ConfigType.WG) }

    val wgText = remember(tunnelConfig) { tunnelConfig.toWgConfig().toWgQuickString(true) }
    val amText = remember(tunnelConfig) { tunnelConfig.toAmConfig().toAwgQuickString(true, false) }

    val isWgTooLarge by remember(wgText) { derivedStateOf { wgText.isTextTooLargeForQr() } }
    val isAmTooLarge by remember(amText) { derivedStateOf { amText.isTextTooLargeForQr() } }

    val qrCodeText by
        remember(selectedOption, wgText, amText) {
            derivedStateOf {
                when (selectedOption) {
                    ConfigType.AM -> amText
                    ConfigType.WG -> wgText
                }
            }
        }

    LaunchedEffect(isWgTooLarge, isAmTooLarge) {
        if (isWgTooLarge && isAmTooLarge) {
            onDismiss()
            context.showToast(R.string.text_too_large_for_qr)
        } else if (isAmTooLarge && selectedOption == ConfigType.AM) {
            selectedOption = ConfigType.WG
            context.showToast(R.string.text_too_large_for_qr)
        } else if (isWgTooLarge && selectedOption == ConfigType.WG) {
            selectedOption = ConfigType.AM
            context.showToast(R.string.text_too_large_for_qr)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
    ) {
        val qrCodePainter = rememberQrCodePainter(data = qrCodeText, options = createQrOptions())
        Image(
            painter = qrCodePainter,
            contentDescription = stringResource(R.string.show_qr),
            modifier =
                Modifier.size(300.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp)
                    .background(Color.White),
        )
        ConfigTypeSelector(
            selectedOption = selectedOption,
            onOptionSelected = { newOption ->
                val isTooLarge =
                    when (newOption) {
                        ConfigType.AM -> isAmTooLarge
                        ConfigType.WG -> isWgTooLarge
                    }
                if (isTooLarge) {
                    context.showToast(R.string.text_too_large_for_qr)
                } else {
                    selectedOption = newOption
                }
            },
            isWgTooLarge = isWgTooLarge,
            isAmTooLarge = isAmTooLarge,
        )
    }
}

@Composable
private fun ConfigTypeSelector(
    selectedOption: ConfigType,
    onOptionSelected: (ConfigType) -> Unit,
    isWgTooLarge: Boolean,
    isAmTooLarge: Boolean,
) {
    MultiChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        ConfigType.entries.sortedDescending().forEachIndexed { index, entry ->
            val isActive = selectedOption == entry
            val isEnabled =
                when (entry) {
                    ConfigType.AM -> !isAmTooLarge
                    ConfigType.WG -> !isWgTooLarge
                }
            val typeName =
                stringResource(
                    when (entry) {
                        ConfigType.AM -> R.string.amnezia
                        ConfigType.WG -> R.string.wireguard
                    }
                )
            val activeContainerColor = Color.White
            val inactiveContainerColor = Color.White
            val activeContentColor = if (isEnabled) Color.Black else Color.Gray
            val inactiveContentColor = if (isEnabled) Color.Black else Color.Gray
            SegmentedButton(
                shape =
                    SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = ConfigType.entries.size,
                        baseShape = RoundedCornerShape(8.dp),
                    ),
                icon = {
                    SegmentedButtonDefaults.Icon(
                        active = isActive,
                        activeContent = {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = stringResource(R.string.select),
                                tint =
                                    if (isEnabled) MaterialTheme.colorScheme.primary
                                    else Color.Gray,
                                modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                            )
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VpnKey,
                            contentDescription = typeName,
                            tint = if (isEnabled) Color.Black else Color.Gray,
                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                        )
                    }
                },
                colors =
                    SegmentedButtonDefaults.colors(
                        activeContainerColor = activeContainerColor,
                        inactiveContainerColor = inactiveContainerColor,
                        activeContentColor = activeContentColor,
                        inactiveContentColor = inactiveContentColor,
                    ),
                onCheckedChange = { onOptionSelected(entry) },
                checked = isActive,
            ) {
                Text(
                    text = typeName,
                    color = if (isEnabled) Color.Black else Color.Gray,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

private fun createQrOptions(): QrOptions = QrOptions {
    shapes {
        darkPixel = QrPixelShape.circle()
        ball = QrBallShape.circle()
        frame = QrFrameShape.roundCorners(0.2f)
    }
    colors {
        dark = QrBrush.solid(Color.Black)
        frame = QrBrush.solid(Color.Black)
        ball = QrBrush.solid(Color.Black)
    }
    errorCorrectionLevel = QrErrorCorrectionLevel.Medium
}
