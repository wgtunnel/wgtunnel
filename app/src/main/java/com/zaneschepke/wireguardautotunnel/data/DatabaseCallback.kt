package com.zaneschepke.wireguardautotunnel.data

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import javax.inject.Inject
import javax.inject.Provider

class DatabaseCallback @Inject constructor(private val databaseProvider: Provider<AppDatabase>) :
    RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        db.execSQL("INSERT INTO proxy_settings DEFAULT VALUES")
        db.execSQL("INSERT INTO Settings DEFAULT VALUES")
    }
}
