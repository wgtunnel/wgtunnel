package com.zaneschepke.wireguardautotunnel.util

object Constants {

    const val MAX_LOG_SIZE = 10_000L
    const val BASE_PACKAGE = "com.zaneschepke.wireguardautotunnel"

    const val BASE_LOG_FILE_NAME = "wg_tunnel_logs"

    const val MANUAL_TUNNEL_CONFIG_ID = 0
    const val BATTERY_SAVER_WATCHER_WAKE_LOCK_TIMEOUT = 10 * 60 * 1_000L // 10 minutes

    const val CONF_FILE_EXTENSION = ".conf"
    const val ZIP_FILE_EXTENSION = ".zip"
    const val URI_CONTENT_SCHEME = "content"
    private const val TEXT_MIME_TYPE = "text/plain"
    const val ZIP_FILE_MIME_TYPE = "application/zip"
    const val ALLOWED_TV_FILE_TYPES = "${TEXT_MIME_TYPE}|${ZIP_FILE_MIME_TYPE}"
    const val ALL_FILE_TYPES = "*/*"
    const val GOOGLE_TV_EXPLORER_STUB = "com.google.android.tv.frameworkpackagestubs"
    const val ANDROID_TV_EXPLORER_STUB = "com.android.tv.frameworkpackagestubs"
    const val VPN_SETTINGS_PACKAGE = "android.net.vpn.SETTINGS"
    const val SYSTEM_EXEMPT_SERVICE_TYPE_ID = 1024

    const val DEFAULT_EXPORT_FILE_NAME = "wgtunnel-export.zip"

    const val SUBSCRIPTION_TIMEOUT = 5_000L

    const val DEFAULT_PING_IP = "1.1.1.1"
    const val PING_TIMEOUT = 5_000L
    const val PING_INTERVAL = 60_000L
    const val PING_COOLDOWN = PING_INTERVAL * 60 // one hour

    val amProperties = listOf("Jc", "Jmin", "Jmax", "S1", "S2", "H1", "H2", "H3", "H4")
    const val QR_CODE_NAME_PROPERTY = "# Name ="

    const val FDROID_FLAVOR = "fdroid"
    const val GOOGLE_PLAY_FLAVOR = "google"
    const val STANDALONE_FLAVOR = "standalone"
    const val RELEASE = "release"

    const val BASE_RELEASE_URL = "https://github.com/wgtunnel/wgtunnel/releases/tag/"
}
