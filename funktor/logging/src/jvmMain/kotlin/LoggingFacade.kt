package io.peekandpoke.funktor.logging

class LoggingFacade(logsStorage: Lazy<LogsStorage>) {
    val logsStorage: LogsStorage by logsStorage
}
