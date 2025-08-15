package com.zaneschepke.wireguardautotunnel.ui.screens.settings.backend

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Forward5
import androidx.compose.material.icons.outlined.Http
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.entity.Settings
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendMode
import com.zaneschepke.wireguardautotunnel.ui.common.SectionDivider
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItemLabel
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionLabelType
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.backend.compoents.BackendModeBottomSheet
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.backend.compoents.authOptionsItem
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.backend.compoents.bindAddressItem
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.ui.state.AppViewState
import com.zaneschepke.wireguardautotunnel.util.extensions.asTitleString
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun BackendModeScreen(uiState: AppUiState, viewModel: AppViewModel, appViewState: AppViewState) {
    val context = LocalContext.current

    val showBottomSheet by remember(appViewState.bottomSheet) { derivedStateOf { appViewState.bottomSheet == AppViewState.BottomSheet.BACKEND } }
    val showProxyOptions by remember(uiState.appSettings.backendMode) { derivedStateOf { uiState.appSettings.backendMode == BackendMode.PROXIED_USERSPACE } }
    val showAuthSettings by remember(uiState.appSettings.socks5ProxyEnabled, uiState.appSettings.httpProxyEnabled) {
        derivedStateOf { uiState.appSettings.socks5ProxyEnabled || uiState.appSettings.httpProxyEnabled }
    }

    if(showBottomSheet) BackendModeBottomSheet(uiState.appSettings, viewModel)

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
        modifier = Modifier.fillMaxSize().padding(top = 24.dp).padding(horizontal = 24.dp),
    ) {
        SurfaceSelectionGroupButton(
            listOf(
                SelectionItem(
                    leading = {
                        Icon(ImageVector.vectorResource(R.drawable.sdk), contentDescription = null)
                    },
                    trailing = {
                        Icon(Icons.Outlined.ExpandMore, contentDescription = stringResource(R.string.select))
                    },
                    title = {
                        SelectionItemLabel(
                            stringResource(R.string.backend_mode),
                            SelectionLabelType.TITLE
                        )
                    },
                    description = {
                        SelectionItemLabel(
                            stringResource(R.string.current_template, uiState.appSettings.backendMode.asTitleString(context)),
                            SelectionLabelType.DESCRIPTION
                        )
                    },

                    onClick = {
                        viewModel.handleEvent(AppEvent.SetBottomSheet(AppViewState.BottomSheet.BACKEND))
                    }
                )
            )
        )
        if(showProxyOptions) {
            SurfaceSelectionGroupButton(
                buildList {
                    add(
                        SelectionItem(
                        leading = { Icon(Icons.Outlined.Forward5, contentDescription = null) },
                        title = {
                            SelectionItemLabel(stringResource(R.string.socks_5_proxy), SelectionLabelType.TITLE)
                        },
                        trailing = {
                            ScaledSwitch(
                                checked = uiState.appSettings.socks5ProxyEnabled,
                                onClick = { viewModel.handleEvent(AppEvent.ToggleSocks5Proxy) },
                            )
                        },
                        onClick = { viewModel.handleEvent(AppEvent.ToggleSocks5Proxy) },
                    ))
                    if (uiState.appSettings.socks5ProxyEnabled) {
                        val bindAddressValue by remember(uiState.appSettings.socks5ProxyBindAddress) {
                            derivedStateOf { if(uiState.appSettings.socks5ProxyBindAddress == Settings.SOCKS5_PROXY_DEFAULT_BIND_ADDRESS) "" else
                                uiState.appSettings.socks5ProxyBindAddress }
                        }
                        add(bindAddressItem(bindAddressValue, "Socks5 Bind Address", "(defaults to ${Settings.SOCKS5_PROXY_DEFAULT_BIND_ADDRESS})") {
                            viewModel.handleEvent(AppEvent.SetSocks5BindAddress(it))
                        })
                    }
                    add(
                        SelectionItem(
                            leading = { Icon(Icons.Outlined.Http, contentDescription = null) },
                            title = {
                                SelectionItemLabel(stringResource(R.string.http_proxy), SelectionLabelType.TITLE)
                            },
                            trailing = {
                                ScaledSwitch(
                                    checked = uiState.appSettings.httpProxyEnabled,
                                    onClick = { viewModel.handleEvent(AppEvent.ToggleHttpProxy) },
                                )
                            },
                            onClick = { viewModel.handleEvent(AppEvent.ToggleHttpProxy) },
                        ))
                    if (uiState.appSettings.httpProxyEnabled) {
                        val bindAddressValue by remember(uiState.appSettings.httpProxyBindAddress) {
                            derivedStateOf { if(uiState.appSettings.httpProxyBindAddress == Settings.HTTP_PROXY_DEFAULT_BIND_ADDRESS) "" else
                            uiState.appSettings.httpProxyBindAddress }
                        }
                        add(bindAddressItem(bindAddressValue, "HTTP Bind Address", "(defaults to ${Settings.HTTP_PROXY_DEFAULT_BIND_ADDRESS})") {
                            viewModel.handleEvent(AppEvent.SetHttpProxyBindAddress(it))
                        })
                    }
                }
            )
            if(showAuthSettings) {
                SectionDivider()
                SurfaceSelectionGroupButton(
                    listOf(
                        authOptionsItem()
                    )
                )
            }
        }
    }
}