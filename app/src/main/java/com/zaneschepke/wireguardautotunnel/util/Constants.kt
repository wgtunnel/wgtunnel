package com.zaneschepke.wireguardautotunnel.util

object Constants {
    const val BASE_PACKAGE = "com.zaneschepke.wireguardautotunnel"

    const val BASE_LOG_FILE_NAME = "wg_tunnel_logs"

    const val VPN_SETTINGS_PACKAGE = "android.net.vpn.SETTINGS"
    const val SYSTEM_EXEMPT_SERVICE_TYPE_ID = 1 shl 10
    const val SPECIAL_USE_SERVICE_TYPE_ID = 1 shl 30

    const val QR_CODE_NAME_PROPERTY = "# Name ="

    const val FDROID_FLAVOR = "fdroid"
    const val GOOGLE_PLAY_FLAVOR = "google"
    const val STANDALONE_FLAVOR = "standalone"
    const val RELEASE = "release"

    const val BASE_RELEASE_URL = "https://github.com/wgtunnel/wgtunnel/releases/tag/"
}
