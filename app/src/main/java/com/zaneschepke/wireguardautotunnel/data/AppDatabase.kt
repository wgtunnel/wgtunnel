package com.zaneschepke.wireguardautotunnel.data

import androidx.room.*
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import com.zaneschepke.wireguardautotunnel.data.dao.ProxySettingsDao
import com.zaneschepke.wireguardautotunnel.data.dao.SettingsDao
import com.zaneschepke.wireguardautotunnel.data.dao.TunnelConfigDao
import com.zaneschepke.wireguardautotunnel.data.entity.ProxySettings
import com.zaneschepke.wireguardautotunnel.data.entity.Settings
import com.zaneschepke.wireguardautotunnel.data.entity.TunnelConfig

@Database(
    entities = [Settings::class, TunnelConfig::class, ProxySettings::class],
    version = 20,
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
            AutoMigration(from = 18, to = 19, spec = PingMigration::class),
            AutoMigration(from = 19, to = 20, spec = ProxyMigration::class),
        ],
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingDao(): SettingsDao

    abstract fun tunnelConfigDoa(): TunnelConfigDao

    abstract fun proxySettingsDoa(): ProxySettingsDao
}

@DeleteColumn(tableName = "Settings", columnName = "default_tunnel")
@DeleteColumn(tableName = "Settings", columnName = "is_battery_saver_enabled")
class RemoveLegacySettingColumnsMigration : AutoMigrationSpec

@DeleteColumn(tableName = "Settings", columnName = "is_auto_tunnel_paused")
class RemoveTunnelPauseMigration : AutoMigrationSpec

@DeleteColumn(tableName = "Settings", columnName = "is_wifi_by_shell_enabled")
class WifiDetectionMigration : AutoMigrationSpec

@DeleteColumn.Entries(
    DeleteColumn(tableName = "TunnelConfig", columnName = "ping_interval"),
    DeleteColumn(tableName = "TunnelConfig", columnName = "ping_cooldown"),
    DeleteColumn(tableName = "Settings", columnName = "split_tunnel_apps"),
)
@RenameColumn.Entries(
    RenameColumn(
        tableName = "TunnelConfig",
        fromColumnName = "is_ping_enabled",
        toColumnName = "restart_on_ping_failure",
    ),
    RenameColumn(
        tableName = "TunnelConfig",
        fromColumnName = "ping_ip",
        toColumnName = "ping_target",
    ),
)
class PingMigration : AutoMigrationSpec

@DeleteColumn.Entries(
    DeleteColumn(tableName = "Settings", columnName = "is_amnezia_enabled"),
    DeleteColumn(tableName = "Settings", columnName = "is_vpn_kill_switch_enabled"),
    DeleteColumn(tableName = "Settings", columnName = "is_kernel_kill_switch_enabled"),
    DeleteColumn(tableName = "Settings", columnName = "is_kernel_enabled"),
)
class ProxyMigration : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.execSQL("INSERT INTO proxy_settings DEFAULT VALUES")
    }
}
