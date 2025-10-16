package com.zaneschepke.logcatter.model

import java.time.Instant

data class LogMessage(
    val time: String,
    val pid: String,
    val tid: String,
    val level: LogLevel,
    val tag: String,
    val message: String,
) {
    override fun toString(): String {
        return "$time $pid $tid $level $tag message= $message"
    }

    companion object {
        fun from(logcatLine: String): LogMessage {
            return if (logcatLine.contains("---------")) {
                LogMessage(
                    Instant.now().toString(),
                    "0",
                    "0",
                    LogLevel.VERBOSE,
                    "System",
                    logcatLine,
                )
            } else {
                val parts = logcatLine.trim().split(" ").filter { it.isNotEmpty() }
                val epochParts = parts[0].split(".").map { it.toLong() }
                val message = parts.subList(5, parts.size).joinToString(" ")
                LogMessage(
                    Instant.ofEpochSecond(epochParts[0], epochParts[1]).toString(),
                    parts[1],
                    parts[2],
                    LogLevel.fromSignifier(parts[3]),
                    parts[4],
                    message,
                )
            }
        }
    }
}
