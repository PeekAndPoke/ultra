package io.peekandpoke.funktor.logging

/** Facade providing access to log storage operations. */
class LoggingFacade(logsStorage: Lazy<LogsStorage>) {
    val logsStorage: LogsStorage by logsStorage
}
