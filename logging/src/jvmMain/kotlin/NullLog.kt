package de.peekandpoke.ultra.logging

class NullLog : Log {

    override fun log(level: LogLevel, message: String) {
        // noop
    }
}
