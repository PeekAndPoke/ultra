package io.peekandpoke.ultra.log

import org.slf4j.Logger
import java.time.ZonedDateTime

/**
 * A [LogAppender] that delegates to an SLF4J [Logger].
 *
 * Each [LogLevel] is mapped to its SLF4J counterpart. [LogLevel.OFF] and [LogLevel.ALL]
 * are silently ignored since they have no SLF4J equivalent.
 *
 * @property slf4j the SLF4J logger to delegate to.
 */
class Slf4jAppender(private val slf4j: Logger) : LogAppender {

    /**
     * Forwards the log entry to SLF4J at the matching severity level.
     *
     * The message is prefixed with the abbreviated [loggerName].
     */
    override suspend fun append(ts: ZonedDateTime, level: LogLevel, message: String, loggerName: String) {

        val formatted = LogAppender.formatLoggerName(loggerName) + " " + message

        when (level) {
            LogLevel.ERROR -> slf4j.error(formatted)
            LogLevel.WARNING -> slf4j.warn(formatted)
            LogLevel.INFO -> slf4j.info(formatted)
            LogLevel.DEBUG -> slf4j.debug(formatted)
            LogLevel.TRACE -> slf4j.trace(formatted)
            else -> { /* noop */
            }
        }
    }
}
