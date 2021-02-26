package de.peekandpoke.ultra.logging

import org.slf4j.Logger
import java.time.ZonedDateTime

class Slf4jAppender(private val slf4j: Logger) : LogAppender {

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
