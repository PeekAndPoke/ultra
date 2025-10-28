package de.peekandpoke.funktor.insights.collectors

import de.peekandpoke.funktor.insights.InsightsCollector
import de.peekandpoke.funktor.insights.InsightsCollectorData
import de.peekandpoke.funktor.insights.RoutingInstrumentation
import de.peekandpoke.funktor.insights.gui.InsightsGuiTemplate
import de.peekandpoke.ultra.semanticui.icon
import io.ktor.server.application.*
import kotlinx.html.pre

class RoutingCollector : InsightsCollector {

    data class Data(
        val trace: String? = null,
    ) : InsightsCollectorData {

        override fun renderDetails(template: InsightsGuiTemplate) = with(template) {
            menu {
                icon.compass_outline()
                +"Routing"
            }

            content {

                pre { +(trace ?: "???") }

                json(this@Data)
            }
        }
    }

    private var data: Data = Data()

    override fun finish(call: ApplicationCall): InsightsCollectorData {
        val trace = call.attributes.getOrNull(RoutingInstrumentation.Key)

        return data.copy(
            trace = trace?.buildText()
        )
    }
}
