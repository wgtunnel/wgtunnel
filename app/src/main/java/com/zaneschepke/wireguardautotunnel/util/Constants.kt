package com.zaneschepke.wireguardautotunnel.util

object Constants {
    const val BASE_PACKAGE = "com.zaneschepke.wireguardautotunnel"

    const val BASE_LOG_FILE_NAME = "wg_tunnel_logs"

    const val BATTERY_SAVER_WATCHER_WAKE_LOCK_TIMEOUT = 10 * 60 * 1_000L // 10 minutes
    const val VPN_SETTINGS_PACKAGE = "android.net.vpn.SETTINGS"
    const val SYSTEM_EXEMPT_SERVICE_TYPE_ID = 1 shl 10
    const val SPECIAL_USE_SERVICE_TYPE_ID = 1 shl 30

    const val DEFAULT_EXPORT_FILE_NAME = "wgtunnel-export.zip"

    const val SUBSCRIPTION_TIMEOUT = 5_000L

    const val DEFAULT_PING_IP = "1.1.1.1"
    const val PING_TIMEOUT: Int = 5_000

    const val PING_ATTEMPTS: Int = 3
    const val PING_INTERVAL = 30

    val amProperties = listOf("Jc", "Jmin", "Jmax", "S1", "S2", "H1", "H2", "H3", "H4")
    const val QR_CODE_NAME_PROPERTY = "# Name ="

    const val FDROID_FLAVOR = "fdroid"
    const val GOOGLE_PLAY_FLAVOR = "google"
    const val STANDALONE_FLAVOR = "standalone"
    const val RELEASE = "release"

    const val BASE_RELEASE_URL = "https://github.com/wgtunnel/wgtunnel/releases/tag/"
}
