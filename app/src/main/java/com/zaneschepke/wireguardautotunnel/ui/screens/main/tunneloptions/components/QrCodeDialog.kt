package com.zaneschepke.wireguardautotunnel.ui.screens.main.tunneloptions.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.util.extensions.setScreenBrightness
import io.github.alexzhirkevich.qrose.options.QrBallShape
import io.github.alexzhirkevich.qrose.options.QrBrush
import io.github.alexzhirkevich.qrose.options.QrErrorCorrectionLevel
import io.github.alexzhirkevich.qrose.options.QrFrameShape
import io.github.alexzhirkevich.qrose.options.QrOptions
import io.github.alexzhirkevich.qrose.options.QrPixelShape
import io.github.alexzhirkevich.qrose.options.circle
import io.github.alexzhirkevich.qrose.options.roundCorners
import io.github.alexzhirkevich.qrose.options.solid
import io.github.alexzhirkevich.qrose.rememberQrCodePainter

@Composable
fun QrCodeDialog(tunnelConf: TunnelConf, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? MainActivity

    // Handle screen brightness
    DisposableEffect(Unit) {
        activity?.setScreenBrightness(1.0f)
        onDispose { activity?.setScreenBrightness(-1f) }
    }

    QrCodeAlertDialog(tunnelConf = tunnelConf, onDismiss = onDismiss)
}

@Composable
private fun QrCodeAlertDialog(tunnelConf: TunnelConf, onDismiss: () -> Unit) {
    Surface(color = Color.White, tonalElevation = 0.dp) {
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
                    text = tunnelConf.name,
                    color = Color.Black,
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = { QrCodeContent(tunnelConf = tunnelConf) },
            properties = DialogProperties(usePlatformDefaultWidth = true),
        )
    }
}

@Composable
private fun QrCodeContent(tunnelConf: TunnelConf) {
    var selectedOption by remember { mutableStateOf(ConfigType.WG) }
    val qrCodeText =
        when (selectedOption) {
            ConfigType.AM -> tunnelConf.toAmConfig().toAwgQuickString(true)
            ConfigType.WG -> tunnelConf.toWgConfig().toWgQuickString(true)
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
            onOptionSelected = { selectedOption = it },
        )
    }
}

@Composable
private fun ConfigTypeSelector(selectedOption: ConfigType, onOptionSelected: (ConfigType) -> Unit) {
    MultiChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        ConfigType.entries.sortedDescending().forEachIndexed { index, entry ->
            val isActive = selectedOption == entry
            val typeName =
                stringResource(
                    when (entry) {
                        ConfigType.AM -> R.string.amnezia
                        ConfigType.WG -> R.string.wireguard
                    }
                )
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
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                            )
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VpnKey,
                            contentDescription = typeName,
                            tint = Color.Black,
                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                        )
                    }
                },
                colors =
                    SegmentedButtonDefaults.colors(
                        activeContainerColor = Color.White,
                        inactiveContainerColor = Color.White,
                        activeContentColor = Color.Black,
                        inactiveContentColor = Color.Black,
                    ),
                onCheckedChange = { onOptionSelected(entry) },
                checked = isActive,
            ) {
                Text(
                    text = typeName,
                    color = Color.Black,
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
