package io.peekandpoke.ultra.log

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A [LogAppender] that delegates to SLF4J.
 *
 * One SLF4J [Logger] is resolved per originating class, so per-package levels, appender routing and
 * IDE log navigation all work against the real origin. [LoggerFactory] caches the instances.
 *
 * [LogEvent.error] is handed to SLF4J's throwable channel rather than rendered into the message, so
 * structured backends can group and index it.
 *
 * Accepts every level, leaving SLF4J's own configuration authoritative. A ctor parameter is
 * deliberately avoided so `singleton(Slf4jAppender::class)` works: kontainer resolves every
 * primary-constructor parameter as a service. Override [minLevel] in a subclass to bound it.
 */
open class Slf4jAppender : LogAppender {

    /** Resolves the SLF4J logger for [loggerName]. Overridable so tests can observe the routing. */
    internal open fun loggerFor(loggerName: String): Logger = LoggerFactory.getLogger(loggerName)

    /**
     * Forwards the event to SLF4J at the matching severity, under a logger named after the origin.
     *
     * [LogEvent.ts] is not forwarded; SLF4J stamps each event itself when it receives it.
     */
    override suspend fun append(event: LogEvent) {

        val slf4j = loggerFor(event.loggerName)
        val message = event.message
        val error = event.error

        when (event.level) {
            LogLevel.ERROR -> slf4j.error(message, error)
            LogLevel.WARNING -> slf4j.warn(message, error)
            LogLevel.INFO -> slf4j.info(message, error)
            LogLevel.DEBUG -> slf4j.debug(message, error)
            LogLevel.TRACE -> slf4j.trace(message, error)
            // thresholds, not event levels - UltraLogManager never dispatches these
            LogLevel.OFF, LogLevel.ALL -> Unit
        }
    }
}
