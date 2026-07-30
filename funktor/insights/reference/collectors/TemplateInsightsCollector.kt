package io.peekandpoke.funktor.insights.collectors

import io.ktor.server.application.*
import io.peekandpoke.funktor.insights.InsightsCollector
import io.peekandpoke.funktor.insights.InsightsCollectorData
import io.peekandpoke.funktor.insights.gui.InsightsBarTemplate
import io.peekandpoke.funktor.insights.gui.InsightsGuiTemplate
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.ui
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
