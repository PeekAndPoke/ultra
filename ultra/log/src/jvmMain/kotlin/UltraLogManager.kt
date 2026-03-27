package io.peekandpoke.ultra.log

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import kotlin.reflect.KClass

/**
 * Central log manager that dispatches log messages to registered [LogAppender]s.
 *
 * Appenders are invoked asynchronously on an unconfined coroutine scope so that
 * logging does not block the caller.
 *
 * @param appenders the initial list of appenders to dispatch log events to.
 */
class UltraLogManager(appenders: List<LogAppender>) {

    companion object {
        private val scopeJob = SupervisorJob()
        private val scope = CoroutineScope(Dispatchers.Unconfined + scopeJob)
    }

    private val appenders = appenders.toMutableList()

    /**
     * Creates a [Log] instance bound to the given [cls].
     *
     * @param cls the class requesting the logger, used to derive the logger name.
     * @return a new [LogImpl] that delegates to this manager.
     */
    fun getLogger(cls: KClass<*>): Log {
        return LogImpl(cls, this)
    }

    /**
     * Dispatches a log event to all registered appenders.
     *
     * The current timestamp is captured once and shared across all appenders.
     *
     * @param level      the severity level of the log event.
     * @param message    the log message text.
     * @param loggerName the fully-qualified name of the originating logger.
     */
    fun log(level: LogLevel, message: String, loggerName: String) {

        val now = ZonedDateTime.now()

        scope.launch {
            appenders.forEach {
                it.append(now, level, message, loggerName)
            }
        }
    }

    /**
     * Registers an additional [appender] to receive future log events.
     *
     * @param appender the appender to add.
     */
    fun add(appender: LogAppender) {
        appenders.add(appender)
    }
}
