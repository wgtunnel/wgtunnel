package com.zaneschepke.wireguardautotunnel.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zaneschepke.wireguardautotunnel.data.domain.Settings
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun save(t: Settings)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun saveAll(t: List<Settings>)

	@Query("SELECT * FROM settings WHERE id=:id")
	suspend fun getById(id: Long): Settings?

	@Query("SELECT * FROM settings")
	suspend fun getAll(): List<Settings>

	@Query("SELECT * FROM settings LIMIT 1")
	fun getSettingsFlow(): Flow<Settings>

	@Query("SELECT * FROM settings")
	fun getAllFlow(): Flow<MutableList<Settings>>

	@Delete
	suspend fun delete(t: Settings)

	@Query("SELECT COUNT('id') FROM settings")
	suspend fun count(): Long
}
