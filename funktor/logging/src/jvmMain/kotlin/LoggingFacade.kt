package de.peekandpoke.ktorfx.logging

class LoggingFacade(logsStorage: Lazy<LogsStorage>) {
    val logsStorage: LogsStorage by logsStorage
}
