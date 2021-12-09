package de.peekandpoke.ultra.logging

object NullLog : Log {

    override fun log(level: LogLevel, message: String) {
        // noop
    }
}
