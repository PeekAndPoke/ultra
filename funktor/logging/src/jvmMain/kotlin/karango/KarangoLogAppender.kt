package de.peekandpoke.funktor.logging.karango

import de.peekandpoke.ultra.common.network.NetworkUtils
import de.peekandpoke.ultra.logging.LogAppender
import de.peekandpoke.ultra.logging.LogLevel
import java.time.ZonedDateTime

class KarangoLogAppender(
    repo: Lazy<KarangoLogRepository>,
    private val minLevel: LogLevel,
    private val serverName: String = NetworkUtils.getHostNameOrDefault(),
) : LogAppender {

    val repo by repo

    override suspend fun append(ts: ZonedDateTime, level: LogLevel, message: String, loggerName: String) {

        if (level.severity >= minLevel.severity) {

            val entry = KarangoLogEntry(
                createdAt = System.currentTimeMillis(),
                level = level,
                message = message,
                loggerName = loggerName,
                server = serverName,
            )

            repo.tryInsert(entry)
        }
    }
}
