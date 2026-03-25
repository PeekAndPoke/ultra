package io.peekandpoke.ultra.log

object NullLog : Log {

    override fun log(level: LogLevel, message: String) {
        // noop
    }
}
