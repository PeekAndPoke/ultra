package de.peekandpoke.funktor.logging.api

import de.peekandpoke.ultra.logging.LogLevel
import kotlinx.serialization.Serializable

@Serializable
data class LogEntryModel(
    /** Unique identifier for this entry */
    val id: String,
    /** Timestamp when the entry was created in epoch millis */
    val createdAt: Long,
    /** Named log level */
    val level: LogLevel,
    /** Severity value of the logged entry... for filtering / sorting */
    val severity: Int,
    /** The log message */
    val message: String?,
    /** The name of the logger that created the entry */
    val loggerName: String?,
    /** The thread that created the name */
    val threadName: String?,
    /** Stacktrace in case of an error */
    val stackTrace: String?,
    /** The name of the server that created the log entry */
    val server: String?,
    /** flag for marking the entry as being a resolved issue */
    val state: State = State.New,
) {
    enum class State {
        New,
        Ack;

        companion object {
            fun except(except: State) = State.entries.toSet().minus(except)
        }
    }

    val shortenedLoggerName by lazy {
        val name = loggerName ?: "n/a"
        val parts = name.split(".")

        if (parts.size < 3) {
            name
        } else {
            val first = parts.first()
            val last = parts.last()
            val rest = parts.drop(1).dropLast(1).map { it.take(1) }

            listOf(first).plus(rest).plus(last).joinToString(".")
        }
    }
}
