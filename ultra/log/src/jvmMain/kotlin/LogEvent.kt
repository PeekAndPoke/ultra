package io.peekandpoke.ultra.log

import java.time.ZonedDateTime

/**
 * A single log event, as handed to every [LogAppender].
 *
 * [error] is carried separately rather than folded into [message], so an appender can pass it to a
 * backend that understands exceptions instead of a pre-rendered stack trace.
 *
 * @param ts         when the event happened, captured on the calling thread before dispatch.
 * @param level      the severity; never [LogLevel.OFF] or [LogLevel.ALL], which are thresholds.
 * @param message    the log text.
 * @param loggerName the fully-qualified name of the originating logger.
 * @param error      the error to report, if any.
 */
data class LogEvent(
    val ts: ZonedDateTime,
    val level: LogLevel,
    val message: String,
    val loggerName: String,
    val error: Throwable? = null,
)
