package io.peekandpoke.ultra.log

import kotlin.reflect.KClass

/**
 * Default [Log] implementation that delegates to a [UltraLogManager].
 *
 * Each instance is bound to the [KClass] of the requesting caller so that
 * log output includes the originating class name.
 *
 * @property caller  the class that requested this logger.
 * @property manager the log manager that dispatches messages to appenders.
 */
class LogImpl internal constructor(private val caller: KClass<*>, private val manager: UltraLogManager) : Log {

    /**
     * Resolved once: [caller] is immutable, and `qualifiedName` is re-derived from the class
     * metadata whenever kotlin-reflect's soft reference is cleared.
     *
     * Callers without a qualified name - anonymous objects and local classes - become `n/a`.
     */
    private val loggerName = caller.qualifiedName ?: "n/a"

    override fun isEnabled(level: LogLevel): Boolean = manager.isEnabled(level)

    /**
     * Forwards the message to the [manager] together with the [caller]'s name.
     *
     * Dispatch goes through [UltraLogManager.log]: appenders run inline on the calling thread until
     * one of them suspends, so this call may return before every appender has seen the message.
     */
    override fun log(level: LogLevel, message: String, error: Throwable?) {
        manager.log(level, message, loggerName, error)
    }
}
