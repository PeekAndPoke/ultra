package io.peekandpoke.funktor.logging.karango

import io.peekandpoke.ultra.common.network.NetworkUtils
import io.peekandpoke.ultra.log.LogAppender
import io.peekandpoke.ultra.log.LogEvent
import io.peekandpoke.ultra.log.LogLevel

/** Log appender that persists entries to ArangoDB via [KarangoLogRepository]. */
class KarangoLogAppender(
    repo: Lazy<KarangoLogRepository>,
    override val minLevel: LogLevel,
    private val serverName: String = NetworkUtils.getHostNameOrDefault(),
) : LogAppender {

    val repo by repo

    // UltraLogManager already honours `minLevel`, so anything arriving here is accepted.
    override suspend fun append(event: LogEvent) {

        val entry = KarangoLogEntry(
            createdAt = event.ts.toInstant().toEpochMilli(),
            level = event.level,
            message = event.message,
            loggerName = event.loggerName,
            stackTrace = event.error?.stackTraceToString(),
            server = serverName,
        )

        repo.tryInsert(entry)
    }
}
