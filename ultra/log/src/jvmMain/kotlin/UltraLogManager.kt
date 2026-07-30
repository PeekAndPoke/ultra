package io.peekandpoke.ultra.log

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass

/**
 * Central log manager that dispatches log messages to registered [LogAppender]s.
 *
 * Dispatch runs on an unconfined coroutine scope, so appenders execute inline on the calling
 * thread up to their first suspension point: an appender that never suspends (or that blocks)
 * holds up the caller for its whole duration.
 *
 * @param appenders the initial list of appenders to dispatch log events to.
 * @param minLevel  a global lower bound; an event less critical than this reaches no appender,
 *   whatever the appenders themselves accept. Defaults to [LogLevel.ALL], i.e. no bound.
 */
class UltraLogManager(
    appenders: List<LogAppender>,
    private val minLevel: LogLevel = LogLevel.ALL,
) {

    /** Dispatch scope shared by every manager in the process; it is never cancelled or awaited. */
    companion object {
        private val scopeJob = SupervisorJob()
        private val scope = CoroutineScope(Dispatchers.Unconfined + scopeJob)
    }

    /**
     * The registered appenders.
     *
     * Copy-on-write because [add] can race an in-flight dispatch: a plain list threw
     * `ConcurrentModificationException` inside the dispatch coroutine, where nothing surfaced it to
     * the caller and the event was simply lost. Iteration here sees a stable snapshot, and appends
     * are rare compared to reads.
     */
    private val appenders = CopyOnWriteArrayList(appenders)

    /**
     * Creates a [Log] instance bound to the given [cls].
     *
     * @param cls the class requesting the logger, used to derive the logger name.
     */
    fun getLogger(cls: KClass<*>): Log {
        return LogImpl(cls, this)
    }

    /**
     * Whether an event at [level] would reach any appender.
     *
     * [LogLevel.OFF] and [LogLevel.ALL] are thresholds rather than levels a message is logged at,
     * so they are never enabled.
     */
    fun isEnabled(level: LogLevel): Boolean {

        if (level == LogLevel.OFF || level == LogLevel.ALL) {
            return false
        }

        if (level.severity < minLevel.severity) {
            return false
        }

        return appenders.any { level.severity >= it.minLevel.severity }
    }

    /**
     * Dispatches a log event to every appender that accepts [level].
     *
     * Returns immediately, doing no work at all, when nothing would consume the event.
     *
     * @param level      the severity level of the log event.
     * @param message    the log message text.
     * @param loggerName the fully-qualified name of the originating logger.
     * @param error      the error to report, if any.
     */
    fun log(level: LogLevel, message: String, loggerName: String, error: Throwable? = null) {

        if (!isEnabled(level)) {
            return
        }

        // captured on the caller, so every appender reports the same time even if dispatch is queued
        val event = LogEvent(
            ts = ZonedDateTime.now(),
            level = level,
            message = message,
            loggerName = loggerName,
            error = error,
        )

        scope.launch {
            appenders.forEach { appender ->

                if (level.severity < appender.minLevel.severity) {
                    return@forEach
                }

                try {
                    appender.append(event)
                } catch (e: CancellationException) {
                    // never swallow cancellation - it is how the scope is torn down
                    throw e
                } catch (e: Exception) {
                    // One broken appender must not hide the event from the others. There is nowhere
                    // safe to log this failure - we are the logger - so it goes to stderr.
                    System.err.println(
                        "Log appender [${appender::class.qualifiedName}] failed: $e"
                    )
                }
            }
        }
    }

    /**
     * Registers an additional [appender] to receive future log events.
     */
    // TODO(scan): the kontainer registers this manager as `dynamic`, so an appender added here is
    //             gone with the current kontainer instance
    fun add(appender: LogAppender) {
        appenders.add(appender)
    }
}
