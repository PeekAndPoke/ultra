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
     * Forwards the [message] to the [manager] together with the [caller]'s qualified name.
     */
    override fun log(level: LogLevel, message: String) {
        manager.log(level, message, caller.qualifiedName ?: "n/a")
    }
}
