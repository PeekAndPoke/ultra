package io.peekandpoke.funktor.insights.collectors

import io.ktor.server.application.*
import io.peekandpoke.funktor.core.kontainer
import io.peekandpoke.funktor.insights.InsightsCollector
import io.peekandpoke.funktor.insights.InsightsCollectorData
import io.peekandpoke.ultra.log.LogAppender
import io.peekandpoke.ultra.log.LogEvent
import io.peekandpoke.ultra.log.LogLevel

class LogCollector : InsightsCollector {

    override val key = KEY

    companion object {
        const val KEY = "log"
    }

    class Appender : LogAppender {

        private val lock = Any()
        internal val entries = mutableListOf<Data.Entry>()

        fun getAndClear(): List<Data.Entry> {
            return synchronized(lock) {
                entries.toList().also { entries.clear() }
            }
        }

        override suspend fun append(event: LogEvent) {
            synchronized(lock) {
                entries.add(
                    Data.Entry(
                        level = event.level,
                        text = LogAppender.format(event),
                    )
                )
            }
        }
    }

    /** VUE-REF: `reference/collectors/LogCollector.kt` */
    data class Data(
        val entries: List<Entry>,
    ) : InsightsCollectorData {

        data class Entry(
            val level: LogLevel,
            val text: String,
        )
    }


    override fun finish(call: ApplicationCall): InsightsCollectorData {

        return call.kontainer
            .use(Appender::class) {
                Data(getAndClear())
            }
            ?: Data(listOf())
    }
}
