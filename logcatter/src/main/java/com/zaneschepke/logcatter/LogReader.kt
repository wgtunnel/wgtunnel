package com.zaneschepke.logcatter

import com.zaneschepke.logcatter.model.LogMessage
import kotlinx.coroutines.flow.Flow

interface LogReader {
    fun start()

    fun stop()

    suspend fun zipLogFiles(path: String)

    suspend fun deleteAndClearLogs()

    val bufferedLogs: Flow<LogMessage>
    val liveLogs: Flow<LogMessage>
}
