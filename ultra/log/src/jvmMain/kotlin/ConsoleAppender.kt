package io.peekandpoke.ultra.log

import java.time.ZonedDateTime

/**
 * A [LogAppender] that writes formatted log messages to standard output via [println].
 *
 * Uses [LogAppender.format] to produce the output string.
 */
class ConsoleAppender : LogAppender {

    /**
     * Prints the formatted log entry to the console.
     */
    override suspend fun append(ts: ZonedDateTime, level: LogLevel, message: String, loggerName: String) {
        println(
            LogAppender.format(ts, level, message, loggerName)
        )
    }
}
