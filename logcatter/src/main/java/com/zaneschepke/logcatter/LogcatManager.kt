package com.zaneschepke.logcatter

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.zaneschepke.logcatter.model.LogMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class LogcatManager(pid: Int, logDir: String, maxFileSize: Long, maxFolderSize: Long) :
    LogReader, DefaultLifecycleObserver {
    private val logScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val fileManager = LogFileManager(logDir, maxFileSize, maxFolderSize)
    private val logcatReader = LogcatStreamReader(pid, fileManager)
    private var logJob: Job? = null
    private var isStarted = false

    private val _bufferedLogs =
        MutableSharedFlow<LogMessage>(
            replay = 10_000,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    private val _liveLogs =
        MutableSharedFlow<LogMessage>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override val bufferedLogs: Flow<LogMessage> = _bufferedLogs.asSharedFlow()
    override val liveLogs: Flow<LogMessage> = _liveLogs.asSharedFlow()

    override fun onCreate(owner: LifecycleOwner) {
        // for auto start
        // start()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        stop()
        logScope.cancel()
    }

    override fun start() {
        if (isStarted) return
        stop()
        logJob =
            logScope.launch {
                logcatReader.readLogs().collect { logMessage ->
                    _bufferedLogs.emit(logMessage)
                    _liveLogs.emit(logMessage)
                }
            }
        isStarted = true
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun stop() {
        if (!isStarted) return
        logJob?.cancel()
        logcatReader.stop()
        fileManager.close()
        _bufferedLogs.resetReplayCache()
        isStarted = false
    }

    override suspend fun zipLogFiles(path: String) {
        val wasStarted = isStarted
        stop()
        fileManager.zipLogs(path)
        if (wasStarted) {
            logcatReader.clearLogs()
            start()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun deleteAndClearLogs() {
        val wasStarted = isStarted
        stop()
        _bufferedLogs.resetReplayCache()
        fileManager.deleteAllLogs()
        if (wasStarted) start()
    }
}
