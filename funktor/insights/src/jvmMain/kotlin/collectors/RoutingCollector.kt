package de.peekandpoke.ktorfx.insights.collectors

import de.peekandpoke.kraft.semanticui.icon
import de.peekandpoke.ktorfx.insights.InsightsCollector
import de.peekandpoke.ktorfx.insights.InsightsCollectorData
import de.peekandpoke.ktorfx.insights.RoutingInstrumentation
import de.peekandpoke.ktorfx.insights.gui.InsightsGuiTemplate
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
        return data.copy(
            trace = call.attributes.getOrNull(RoutingInstrumentation.Key)?.buildText()
        )
    }
}
