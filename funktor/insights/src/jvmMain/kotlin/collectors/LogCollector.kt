package de.peekandpoke.ktorfx.insights.collectors

import de.peekandpoke.kraft.semanticui.SemanticFn
import de.peekandpoke.kraft.semanticui.icon
import de.peekandpoke.kraft.semanticui.semantic
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.ktorfx.core.kontainer
import de.peekandpoke.ktorfx.insights.InsightsCollector
import de.peekandpoke.ktorfx.insights.InsightsCollectorData
import de.peekandpoke.ktorfx.insights.gui.InsightsBarTemplate
import de.peekandpoke.ktorfx.insights.gui.InsightsGuiTemplate
import de.peekandpoke.ultra.logging.LogAppender
import de.peekandpoke.ultra.logging.LogLevel
import io.ktor.server.application.*
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

    override fun finish(call: ApplicationCall): InsightsCollectorData {

        return call.kontainer
            .use(Appender::class) {
                Data(getAndClear())
            }
            ?: Data(listOf())
    }
}
