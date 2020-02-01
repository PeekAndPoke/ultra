package de.peekandpoke.ultra.logging

import java.time.ZonedDateTime

interface LogAppender {
    suspend fun append(ts: ZonedDateTime, level: LogLevel, message: String, loggerName: String)

    companion object {

        private val loggerNameLookUp = mutableMapOf<String, String>()

        fun format(ts: ZonedDateTime, level: LogLevel, message: String, loggerName: String): String {

            val name = when {
                loggerName.length <= 30 -> loggerName

                else -> formatLoggerName(loggerName)
            }

            return "${ts.toLocalDate()} ${ts.toLocalTime()} $level - $name - $message"
        }

        fun formatLoggerName(loggerName: String) = loggerNameLookUp.getOrPut(loggerName) {

            val parts = loggerName.split(".")

            if (parts.size == 1) {
                parts[0]
            } else {
                // only use the first character for all but the simple class name
                parts.take(parts.size - 1).map { it[0] }.joinToString(".") + "." + parts.last()
            }
        }
    }
}
