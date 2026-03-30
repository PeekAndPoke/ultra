package io.peekandpoke.ultra.log

/**
 * A logger that dispatches messages at various [LogLevel]s.
 *
 * Provides convenience methods ([error], [warning], [info], [debug], [trace])
 * that delegate to [log] with the appropriate level.
 *
 * @see LogImpl
 * @see NullLog
 */
interface Log {

    /**
     * Logs a [message] at the specified [level].
     *
     * @param level   the severity level for this message.
     * @param message the text to log.
     */
    fun log(level: LogLevel, message: String)

    /**
     * Logs a [message] at [LogLevel.ERROR].
     *
     * @param message the error text to log.
     */
    fun error(message: String) {
        log(LogLevel.ERROR, message)
    }

    /**
     * Logs a [message] at [LogLevel.WARNING].
     *
     * @param message the warning text to log.
     */
    fun warning(message: String) {
        log(LogLevel.WARNING, message)
    }

    /**
     * Logs a [message] at [LogLevel.INFO].
     *
     * @param message the informational text to log.
     */
    fun info(message: String) {
        log(LogLevel.INFO, message)
    }

    /**
     * Logs a [message] at [LogLevel.DEBUG].
     *
     * @param message the debug text to log.
     */
    fun debug(message: String) {
        log(LogLevel.DEBUG, message)
    }

    /**
     * Logs a [message] at [LogLevel.TRACE].
     *
     * @param message the trace text to log.
     */
    fun trace(message: String) {
        log(LogLevel.TRACE, message)
    }
}
