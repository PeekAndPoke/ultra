package io.peekandpoke.funktor.insights.collectors

import io.ktor.server.application.*
import io.peekandpoke.funktor.insights.InsightsCollector
import io.peekandpoke.funktor.insights.InsightsCollectorData
import io.peekandpoke.funktor.insights.RoutingInstrumentation
import io.peekandpoke.funktor.insights.gui.InsightsGuiTemplate
import io.peekandpoke.ultra.semanticui.icon
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
