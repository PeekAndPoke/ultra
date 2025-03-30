package de.peekandpoke.ktorfx.insights.collectors

import de.peekandpoke.kraft.semanticui.icon
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.ktorfx.insights.InsightsCollector
import de.peekandpoke.ktorfx.insights.InsightsCollectorData
import de.peekandpoke.ktorfx.insights.gui.InsightsBarTemplate
import de.peekandpoke.ktorfx.insights.gui.InsightsGuiTemplate
import io.ktor.server.application.*
import kotlinx.html.title

class TemplateInsightsCollector : InsightsCollector {

    data class Data(
        val timeNs: Long? = null,
    ) : InsightsCollectorData {

        override fun renderBar(template: InsightsBarTemplate) {

            if (timeNs != null) {

                val timeMillis: Double = timeNs / 1_000_000.0

                with(template) {

                    left {
                        ui.item {
                            title = "View rendering"

                            when {
                                // TODO: make thresholds configurable
                                timeMillis > 30 -> icon.red.tv()
                                timeMillis > 10 -> icon.yellow.tv()
                                else -> icon.green.tv()
                            }

                            +"%.2f ms".format(timeMillis)
                        }
                    }
                }
            }
        }

        override fun renderDetails(template: InsightsGuiTemplate) = with(template) {

            menu {
                icon.tv()
                +"View"
            }

            content {
                json(this@Data)
            }
        }
    }

    var data = Data()

    override fun finish(call: ApplicationCall): InsightsCollectorData = data

    fun record(timeNs: Long) {
        data = Data(timeNs)
    }
}
