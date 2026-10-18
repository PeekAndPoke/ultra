package io.peekandpoke.ultra.log

/**
 * A [LogAppender] that writes formatted log messages to standard output via [println].
 *
 * Uses [LogAppender.format] to produce the output string.
 *
 * Accepts every level, so that `singleton(ConsoleAppender::class)` stays a valid registration -
 * kontainer resolves every primary-constructor parameter as a service and cannot supply a
 * [LogLevel]. Bound it globally with `ultraLogging(minLevel = ...)`, or override [minLevel] in a
 * subclass for a per-appender bound.
 */
open class ConsoleAppender : LogAppender {

    /**
     * Prints the formatted log entry to the console.
     *
     * The whole entry goes out in one `println` call, so entries printed concurrently from several
     * threads never interleave with each other.
     */
    override suspend fun append(event: LogEvent) {
        println(
            LogAppender.format(event)
        )
    }
}
