package com.zaneschepke.wireguardautotunnel.domain.sideeffect

import com.zaneschepke.wireguardautotunnel.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.util.StringValue
import java.io.File

sealed class GlobalSideEffect {

    data class Snackbar(val message: StringValue) : GlobalSideEffect()

    data class Toast(val message: StringValue) : GlobalSideEffect()

    data object PopBackStack : GlobalSideEffect()

    data class LaunchUrl(val url: String) : GlobalSideEffect()

    data object ConfigChanged : GlobalSideEffect()

    data class RequestVpnPermission(val requestingMode: AppMode, val config: TunnelConfig?) :
        GlobalSideEffect()

    data class InstallApk(val apk: File) : GlobalSideEffect()
}
