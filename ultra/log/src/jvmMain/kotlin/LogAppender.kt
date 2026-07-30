package io.peekandpoke.ultra.log

import io.peekandpoke.ultra.log.LogAppender.Companion.formatLoggerName
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

/**
 * An output sink for log messages.
 *
 * Implementations receive fully-formed log events and are responsible for
 * writing them to a specific destination (console, SLF4J, file, etc.).
 *
 * [UltraLogManager] hands each event to every appender whose [minLevel] accepts it, in registration
 * order, and isolates failures: an appender that throws does not stop the ones after it.
 *
 * @see ConsoleAppender
 * @see Slf4jAppender
 */
interface LogAppender {

    /**
     * The least critical level this appender accepts; anything less critical is not delivered to it.
     *
     * Defaults to [LogLevel.ALL], i.e. accept everything. Severity runs the other way from
     * declaration order, so `minLevel = LogLevel.WARNING` accepts WARNING and ERROR only.
     */
    val minLevel: LogLevel get() = LogLevel.ALL

    /**
     * Writes [event] to this appender's output destination.
     *
     * Suspending is permitted so that an appender can do IO without blocking the logging caller.
     */
    suspend fun append(event: LogEvent)

    /** Log-line formatting helpers, shared by the built-in appenders and usable by custom ones. */
    companion object {

        /**
         * Memoises [formatLoggerName] results, keyed by the unabbreviated logger name.
         *
         * Concurrent because appenders run on whichever thread called `log`. It never evicts, which
         * is bounded when names come from [LogImpl] (one entry per class) but not when a caller
         * passes arbitrary names to [UltraLogManager.log].
         */
        private val loggerNameLookUp = ConcurrentHashMap<String, String>()

        /**
         * Fixed-width timestamp, so every line has the same shape.
         *
         * `LocalTime.toString()` omits the seconds when they are zero and appends nanoseconds when
         * they are not, which made the output impossible to parse positionally.
         */
        private val timestampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

        /**
         * Formats [event] as a complete log line, appending the stack trace of
         * [LogEvent.error] when there is one.
         *
         * Logger names longer than 30 characters are abbreviated via [formatLoggerName].
         */
        fun format(event: LogEvent): String {

            val name = when {
                event.loggerName.length <= 30 -> event.loggerName

                else -> formatLoggerName(event.loggerName)
            }

            val line = "${timestampFormat.format(event.ts)} ${event.level} - $name - ${event.message}"

            // A console has nowhere else to put the trace, so it goes inline. Appenders talking to a
            // backend that understands exceptions should use LogEvent.error instead of this.
            return when (val error = event.error) {
                null -> line
                else -> line + "\n" + error.stackTraceToString()
            }
        }

        /**
         * Abbreviates a fully-qualified logger name by reducing all package segments
         * to their first character while keeping the simple class name intact.
         *
         * For example, `"io.peekandpoke.ultra.log.MyLogger"` becomes `"i.p.u.l.MyLogger"`.
         * Single-segment names are returned unchanged.
         *
         * Results are cached in an internal lookup map for performance.
         */
        fun formatLoggerName(loggerName: String) = loggerNameLookUp.getOrPut(loggerName) {

            val parts = loggerName.split(".")

            if (parts.size == 1) {
                parts[0]
            } else {
                // only use the first character for all but the simple class name. `take(1)` rather
                // than `[0]`, because an empty segment ("a..b.C") has no first character.
                parts.take(parts.size - 1).joinToString(".") { it.take(1) } + "." + parts.last()
            }
        }
    }
}
