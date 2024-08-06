package com.zaneschepke.wireguardautotunnel.data

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import com.zaneschepke.wireguardautotunnel.data.domain.Settings
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig

@Database(
	entities = [Settings::class, TunnelConfig::class],
	version = 9,
	autoMigrations =
	[
		AutoMigration(from = 1, to = 2),
		AutoMigration(from = 2, to = 3),
		AutoMigration(
			from = 3,
			to = 4,
		),
		AutoMigration(
			from = 4,
			to = 5,
		),
		AutoMigration(
			from = 5,
			to = 6,
		),
		AutoMigration(
			from = 6,
			to = 7,
			spec = RemoveLegacySettingColumnsMigration::class,
		),
		AutoMigration(7, 8),
		AutoMigration(8, 9),
	],
	exportSchema = true,
)
@TypeConverters(DatabaseListConverters::class)
abstract class AppDatabase : RoomDatabase() {
	abstract fun settingDao(): SettingsDao

	abstract fun tunnelConfigDoa(): TunnelConfigDao
}

@DeleteColumn(
	tableName = "Settings",
	columnName = "default_tunnel",
)
@DeleteColumn(
	tableName = "Settings",
	columnName = "is_battery_saver_enabled",
)
class RemoveLegacySettingColumnsMigration : AutoMigrationSpec
