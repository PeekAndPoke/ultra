package de.peekandpoke.funktor.logging.karango

import de.peekandpoke.funktor.logging.api.LogEntryModel
import de.peekandpoke.karango.Karango
import de.peekandpoke.ultra.log.LogLevel
import de.peekandpoke.ultra.vault.Storable

@Karango
data class KarangoLogEntry(
    val createdAt: Long,
    val expiresAt: Long = createdAt / 1000,
    val level: LogLevel,
    val severity: Int = level.severity,
    val loggerName: String?,
    val message: String?,
    val threadName: String? = null,
    val stackTrace: String? = null,
    val server: String? = null,
    val state: LogEntryModel.State = LogEntryModel.State.New,
) {
    fun update(entry: LogEntryModel) = copy(
        level = entry.level,
        state = entry.state
    )
}

fun Storable<KarangoLogEntry>.asApiModel() = with(value) {
    LogEntryModel(
        id = _key,
        createdAt = createdAt,
        level = level,
        severity = severity,
        message = message,
        loggerName = loggerName,
        threadName = threadName,
        stackTrace = stackTrace,
        server = server,
        state = state,
    )
}
