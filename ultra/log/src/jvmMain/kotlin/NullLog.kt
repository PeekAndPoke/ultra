package io.peekandpoke.ultra.log

/**
 * A no-op [Log] implementation that silently discards all messages.
 *
 * Useful as a default or placeholder when no actual logging is desired.
 */
object NullLog : Log {

    /** Discards the [message] without producing any output. */
    override fun log(level: LogLevel, message: String) {
        // noop
    }
}
