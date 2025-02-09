package com.zaneschepke.wireguardautotunnel.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException

class DataStoreManager(
	private val context: Context,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
	companion object {
		val locationDisclosureShown = booleanPreferencesKey("LOCATION_DISCLOSURE_SHOWN")
		val batteryDisableShown = booleanPreferencesKey("BATTERY_OPTIMIZE_DISABLE_SHOWN")
		val currentSSID = stringPreferencesKey("CURRENT_SSID")
		val pinLockEnabled = booleanPreferencesKey("PIN_LOCK_ENABLED")
		val tunnelStatsExpanded = booleanPreferencesKey("TUNNEL_STATS_EXPANDED")
		val isLocalLogsEnabled = booleanPreferencesKey("LOCAL_LOGS_ENABLED")
		val locale = stringPreferencesKey("LOCALE")
		val theme = stringPreferencesKey("THEME")
	}

	// preferences
	private val preferencesKey = "preferences"
	private val Context.dataStore by
		preferencesDataStore(
			name = preferencesKey,
		)

	suspend fun init() {
		withContext(ioDispatcher) {
			try {
				context.dataStore.data.first()
			} catch (e: IOException) {
				Timber.Forest.e(e)
			}
		}
	}

	suspend fun <T> saveToDataStore(key: Preferences.Key<T>, value: T) {
		withContext(ioDispatcher) {
			try {
				context.dataStore.edit { it[key] = value }
			} catch (e: IOException) {
				Timber.Forest.e(e)
			} catch (e: Exception) {
				Timber.Forest.e(e)
			}
		}
	}

	suspend fun <T> removeFromDataStore(key: Preferences.Key<T>) {
		withContext(ioDispatcher) {
			try {
				context.dataStore.edit { it.remove(key) }
			} catch (e: IOException) {
				Timber.Forest.e(e)
			} catch (e: Exception) {
				Timber.Forest.e(e)
			}
		}
	}

	fun <T> getFromStoreFlow(key: Preferences.Key<T>) = context.dataStore.data.map { it[key] }

	suspend fun <T> getFromStore(key: Preferences.Key<T>): T? {
		return withContext(ioDispatcher) {
			try {
				context.dataStore.data.map { it[key] }.first()
			} catch (e: IOException) {
				Timber.Forest.e(e)
				null
			}
		}
	}

	fun <T> getFromStoreBlocking(key: Preferences.Key<T>) = runBlocking {
		context.dataStore.data.map { it[key] }.first()
	}

	val preferencesFlow: Flow<Preferences?> = context.dataStore.data.flowOn(ioDispatcher)
}
