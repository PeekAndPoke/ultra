package de.peekandpoke.ultra.logging

import java.time.ZonedDateTime

class ConsoleAppender : LogAppender {

    override suspend fun append(ts: ZonedDateTime, level: LogLevel, message: String, loggerName: String) {
        println(
            LogAppender.format(ts, level, message, loggerName)
        )
    }
}
