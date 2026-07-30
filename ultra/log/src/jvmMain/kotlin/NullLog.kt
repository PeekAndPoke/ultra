package io.peekandpoke.ultra.log

/**
 * A no-op [Log] implementation that silently discards all messages.
 *
 * Useful as a default or placeholder when no actual logging is desired.
 */
object NullLog : Log {

    /** Nothing consumes these messages, so no level is enabled and no lambda message is built. */
    override fun isEnabled(level: LogLevel): Boolean = false

    /** Discards the message without producing any output. */
    override fun log(level: LogLevel, message: String, error: Throwable?) {
        // noop
    }
}
