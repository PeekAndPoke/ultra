package de.peekandpoke.ultra.log

import java.time.ZonedDateTime

class ConsoleAppender : LogAppender {

    override suspend fun append(ts: ZonedDateTime, level: LogLevel, message: String, loggerName: String) {
        println(
            LogAppender.format(ts, level, message, loggerName)
        )
    }
}
