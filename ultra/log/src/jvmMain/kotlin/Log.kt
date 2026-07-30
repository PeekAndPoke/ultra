package io.peekandpoke.ultra.log

/**
 * A logger that dispatches messages at various [LogLevel]s.
 *
 * Provides convenience methods ([error], [warning], [info], [debug], [trace])
 * that delegate to [log] with the appropriate level.
 *
 * Each convenience method has a lambda form. Prefer it whenever building the message costs
 * anything: the block runs only if some appender accepts the level.
 *
 * @see LogImpl
 * @see NullLog
 */
interface Log {

    /**
     * Whether anything would consume a message at [level].
     *
     * Implementations that cannot answer stay permissive and return `true`.
     */
    fun isEnabled(level: LogLevel): Boolean = true

    /**
     * Logs a [message] at the specified [level], optionally reporting [error].
     *
     * [error] is passed on as-is rather than rendered into [message], so appenders can hand it to a
     * backend that understands exceptions.
     */
    fun log(level: LogLevel, message: String, error: Throwable? = null)

    /** Logs the result of [message] at [level], evaluating it only if [level] is enabled. */
    fun log(level: LogLevel, message: () -> String) {
        if (isEnabled(level)) {
            log(level, message(), null)
        }
    }

    /** Logs a [message] at [LogLevel.ERROR]. */
    fun error(message: String) {
        log(LogLevel.ERROR, message)
    }

    /** Logs a [message] at [LogLevel.ERROR], reporting [e] as the cause. */
    fun error(message: String, e: Throwable) {
        log(LogLevel.ERROR, message, e)
    }

    /** Logs a lazily built message at [LogLevel.ERROR]. */
    fun error(message: () -> String) {
        log(LogLevel.ERROR, message)
    }

    /** Logs a [message] at [LogLevel.WARNING]. */
    fun warning(message: String) {
        log(LogLevel.WARNING, message)
    }

    /** Logs a lazily built message at [LogLevel.WARNING]. */
    fun warning(message: () -> String) {
        log(LogLevel.WARNING, message)
    }

    /** Logs a [message] at [LogLevel.INFO]. */
    fun info(message: String) {
        log(LogLevel.INFO, message)
    }

    /** Logs a lazily built message at [LogLevel.INFO]. */
    fun info(message: () -> String) {
        log(LogLevel.INFO, message)
    }

    /** Logs a [message] at [LogLevel.DEBUG]. */
    fun debug(message: String) {
        log(LogLevel.DEBUG, message)
    }

    /** Logs a lazily built message at [LogLevel.DEBUG]. */
    fun debug(message: () -> String) {
        log(LogLevel.DEBUG, message)
    }

    /** Logs a [message] at [LogLevel.TRACE]. */
    fun trace(message: String) {
        log(LogLevel.TRACE, message)
    }

    /** Logs a lazily built message at [LogLevel.TRACE]. */
    fun trace(message: () -> String) {
        log(LogLevel.TRACE, message)
    }
}
