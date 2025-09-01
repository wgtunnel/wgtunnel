package com.zaneschepke.wireguardautotunnel.data

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.zaneschepke.wireguardautotunnel.data.entity.ProxySettings
import com.zaneschepke.wireguardautotunnel.data.entity.Settings
import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

class DatabaseCallback
@Inject
constructor(
    private val databaseProvider: Provider<AppDatabase>,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        applicationScope.launch(ioDispatcher) {
            val db = databaseProvider.get()
            db.settingDao().save(Settings())
            db.proxySettingsDoa().save(ProxySettings())
        }
    }
}
