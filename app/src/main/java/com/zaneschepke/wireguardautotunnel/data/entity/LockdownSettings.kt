package com.zaneschepke.wireguardautotunnel.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lockdown_settings")
data class LockdownSettings(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "bypass_lan", defaultValue = "0") val bypassLan: Boolean = false,
    @ColumnInfo(name = "metered", defaultValue = "0") val metered: Boolean = false,
    @ColumnInfo(name = "dual_stack", defaultValue = "0") val dualStack: Boolean = false,
)
