package de.peekandpoke.ultra.logging

interface Log {

    fun log(level: LogLevel, message: String)

    fun error(message: String) {
        log(LogLevel.ERROR, message)
    }

    fun warning(message: String) {
        log(LogLevel.WARNING, message)
    }

    fun info(message: String) {
        log(LogLevel.INFO, message)
    }

    fun debug(message: String) {
        log(LogLevel.DEBUG, message)
    }

    fun trace(message: String) {
        log(LogLevel.TRACE, message)
    }
}
