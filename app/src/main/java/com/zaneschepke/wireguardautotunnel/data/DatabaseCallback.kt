package com.zaneschepke.wireguardautotunnel.data

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.zaneschepke.wireguardautotunnel.data.entity.ProxySettings
import com.zaneschepke.wireguardautotunnel.data.entity.Settings
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DatabaseCallback @Inject constructor(private val databaseProvider: Provider<AppDatabase>) :
    RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)

        // Launch coroutine to insert default entry
        CoroutineScope(Dispatchers.IO).launch {
            val db = databaseProvider.get()
            db.settingDao().save(Settings())
            db.proxySettingsDoa().save(ProxySettings())
        }
    }
}
