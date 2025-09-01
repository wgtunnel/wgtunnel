package com.zaneschepke.wireguardautotunnel.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_19_20 =
    object : Migration(19, 20) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
            CREATE TABLE IF NOT EXISTS `proxy_settings` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `socks5_proxy_enabled` INTEGER NOT NULL DEFAULT 0,
                `socks5_proxy_bind_address` TEXT,
                `http_proxy_enable` INTEGER NOT NULL DEFAULT 0,
                `http_proxy_bind_address` TEXT,
                `proxy_username` TEXT,
                `proxy_password` TEXT
            )
        """
                    .trimIndent()
            )

            db.execSQL(
                """
            INSERT INTO `proxy_settings` (
                `socks5_proxy_enabled`, `socks5_proxy_bind_address`,
                `http_proxy_enable`, `http_proxy_bind_address`,
                `proxy_username`, `proxy_password`
            )
            VALUES (0, NULL, 0, NULL, NULL, NULL)
        """
                    .trimIndent()
            )
        }
    }
