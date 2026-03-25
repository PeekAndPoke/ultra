package io.peekandpoke.funktor.insights.collectors

import io.ktor.server.application.*
import io.peekandpoke.funktor.core.kontainer
import io.peekandpoke.funktor.insights.InsightsCollector
import io.peekandpoke.funktor.insights.InsightsCollectorData
import io.peekandpoke.funktor.insights.gui.InsightsBarTemplate
import io.peekandpoke.funktor.insights.gui.InsightsGuiTemplate
import io.peekandpoke.ultra.log.LogAppender
import io.peekandpoke.ultra.log.LogLevel
import io.peekandpoke.ultra.semanticui.SemanticFn
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.semantic
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.pre
import kotlinx.html.title
import java.time.ZonedDateTime

class LogCollector : InsightsCollector {

    class Appender : LogAppender {

        private val lock = Any()
        internal val entries = mutableListOf<Data.Entry>()

        fun getAndClear(): List<Data.Entry> {
            return entries.toList().also {
                synchronized(lock) {
                    entries.clear()
                }
            }
        }

        override suspend fun append(ts: ZonedDateTime, level: LogLevel, message: String, loggerName: String) {
            synchronized(lock) {
                entries.add(
                    Data.Entry(
                        level = level,
                        text = LogAppender.format(ts, level, message, loggerName),
                    )
                )
            }
        }
    }

    data class Data(
        val entries: List<Entry>,
    ) : InsightsCollectorData {

        data class Entry(
            val level: LogLevel,
            val text: String,
        )

        override fun renderBar(template: InsightsBarTemplate) = with(template) {
            left {
                ui.item {
                    title = "Logs"

                    icon.list()

                    +"${entries.size}"
                }
            }
        }

        override fun renderDetails(template: InsightsGuiTemplate) = with(template) {

            menu {
                icon.list()
                +"Logs"
            }

            content {

                if (entries.isEmpty()) {
                    ui.message {
                        +"No log entries"
                    }
                } else {
                    entries.forEach { entry ->

                        val bgColor: SemanticFn = when (entry.level) {
                            LogLevel.ALL, LogLevel.TRACE -> semantic { this }
                            LogLevel.DEBUG -> semantic { this }
                            LogLevel.INFO -> semantic { positive }
                            LogLevel.WARNING -> semantic { warning }
                            LogLevel.ERROR, LogLevel.OFF -> semantic { error }
                        }

                        ui.bgColor().message {
                            pre {
                                +entry.text
                            }
                        }
                    }
                }
            }
        }
    }

    override fun finish(call: ApplicationCall): InsightsCollectorData {

        return call.kontainer
            .use(Appender::class) {
                Data(getAndClear())
            }
            ?: Data(listOf())
    }
}
