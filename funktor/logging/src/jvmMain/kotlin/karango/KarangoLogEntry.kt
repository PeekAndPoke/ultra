package io.peekandpoke.funktor.logging.karango

import io.peekandpoke.funktor.logging.api.LogEntryModel
import io.peekandpoke.ultra.log.LogLevel
import io.peekandpoke.ultra.vault.Storable
import io.peekandpoke.ultra.vault.Vault

@Vault
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
