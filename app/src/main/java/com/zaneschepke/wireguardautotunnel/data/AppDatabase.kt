package com.zaneschepke.wireguardautotunnel.data

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import com.zaneschepke.wireguardautotunnel.data.dao.SettingsDao
import com.zaneschepke.wireguardautotunnel.data.dao.TunnelConfigDao
import com.zaneschepke.wireguardautotunnel.data.entity.Settings
import com.zaneschepke.wireguardautotunnel.data.entity.TunnelConfig

@Database(
    entities = [Settings::class, TunnelConfig::class],
    version = 18,
    autoMigrations =
        [
            AutoMigration(from = 1, to = 2),
            AutoMigration(from = 2, to = 3),
            AutoMigration(from = 3, to = 4),
            AutoMigration(from = 4, to = 5),
            AutoMigration(from = 5, to = 6),
            AutoMigration(from = 6, to = 7, spec = RemoveLegacySettingColumnsMigration::class),
            AutoMigration(7, 8),
            AutoMigration(8, 9),
            AutoMigration(9, 10),
            AutoMigration(from = 10, to = 11, spec = RemoveTunnelPauseMigration::class),
            AutoMigration(from = 11, to = 12),
            AutoMigration(from = 12, to = 13),
            AutoMigration(from = 13, to = 14),
            AutoMigration(from = 14, to = 15),
            AutoMigration(from = 15, to = 16),
            AutoMigration(from = 16, to = 17, spec = WifiDetectionMigration::class),
            AutoMigration(from = 17, to = 18),
        ],
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingDao(): SettingsDao

    abstract fun tunnelConfigDoa(): TunnelConfigDao
}

@DeleteColumn(tableName = "Settings", columnName = "default_tunnel")
@DeleteColumn(tableName = "Settings", columnName = "is_battery_saver_enabled")
class RemoveLegacySettingColumnsMigration : AutoMigrationSpec

@DeleteColumn(tableName = "Settings", columnName = "is_auto_tunnel_paused")
class RemoveTunnelPauseMigration : AutoMigrationSpec

@DeleteColumn(tableName = "Settings", columnName = "is_wifi_by_shell_enabled")
class WifiDetectionMigration : AutoMigrationSpec
