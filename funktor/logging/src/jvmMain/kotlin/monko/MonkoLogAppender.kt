package io.peekandpoke.funktor.logging.monko

import io.peekandpoke.funktor.logging.karango.KarangoLogEntry
import io.peekandpoke.ultra.common.network.NetworkUtils
import io.peekandpoke.ultra.log.LogAppender
import io.peekandpoke.ultra.log.LogLevel
import java.time.ZonedDateTime

class MonkoLogAppender(
    repo: Lazy<MonkoLogRepository>,
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
