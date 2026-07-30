package io.peekandpoke.funktor.logging.monko

import io.peekandpoke.funktor.logging.karango.KarangoLogEntry
import io.peekandpoke.ultra.common.network.NetworkUtils
import io.peekandpoke.ultra.log.LogAppender
import io.peekandpoke.ultra.log.LogEvent
import io.peekandpoke.ultra.log.LogLevel

class MonkoLogAppender(
    repo: Lazy<MonkoLogRepository>,
    override val minLevel: LogLevel,
    private val serverName: String = NetworkUtils.getHostNameOrDefault(),
) : LogAppender {

    val repo by repo

    // UltraLogManager already honours `minLevel`, so anything arriving here is accepted.
    override suspend fun append(event: LogEvent) {

        val entry = KarangoLogEntry(
            createdAt = System.currentTimeMillis(),
            level = event.level,
            message = event.message,
            loggerName = event.loggerName,
            server = serverName,
        )

        repo.tryInsert(entry)
    }
}
