package io.peekandpoke.ultra.log

import io.peekandpoke.ultra.log.LogAppender.Companion.formatLoggerName
import java.time.ZonedDateTime

/**
 * An output sink for log messages.
 *
 * Implementations receive fully-formed log events and are responsible for
 * writing them to a specific destination (console, SLF4J, file, etc.).
 *
 * @see ConsoleAppender
 * @see Slf4jAppender
 */
interface LogAppender {
    /**
     * Writes a log entry to this appender's output destination.
     *
     * @param ts         the timestamp of the log event.
     * @param level      the severity level.
     * @param message    the log message text.
     * @param loggerName the fully-qualified name of the originating logger.
     */
    suspend fun append(ts: ZonedDateTime, level: LogLevel, message: String, loggerName: String)

    companion object {

        private val loggerNameLookUp = mutableMapOf<String, String>()

        /**
         * Formats a complete log line including timestamp, level, logger name, and message.
         *
         * Logger names longer than 30 characters are abbreviated via [formatLoggerName].
         *
         * @param ts         the timestamp of the log event.
         * @param level      the severity level.
         * @param message    the log message text.
         * @param loggerName the fully-qualified logger name.
         * @return a formatted log string.
         */
        fun format(ts: ZonedDateTime, level: LogLevel, message: String, loggerName: String): String {

            val name = when {
                loggerName.length <= 30 -> loggerName

                else -> formatLoggerName(loggerName)
            }

            return "${ts.toLocalDate()} ${ts.toLocalTime()} $level - $name - $message"
        }

        /**
         * Abbreviates a fully-qualified logger name by reducing all package segments
         * to their first character while keeping the simple class name intact.
         *
         * For example, `"io.peekandpoke.ultra.log.MyLogger"` becomes `"i.p.u.l.MyLogger"`.
         * Single-segment names are returned unchanged.
         *
         * Results are cached in an internal lookup map for performance.
         *
         * @param loggerName the fully-qualified logger name to abbreviate.
         * @return the abbreviated logger name.
         */
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
