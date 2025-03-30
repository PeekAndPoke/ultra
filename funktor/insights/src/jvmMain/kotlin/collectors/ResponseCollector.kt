package de.peekandpoke.ktorfx.insights.collectors

import de.peekandpoke.kraft.semanticui.icon
import de.peekandpoke.ktorfx.insights.InsightsCollector
import de.peekandpoke.ktorfx.insights.InsightsCollectorData
import de.peekandpoke.ktorfx.insights.gui.InsightsGuiTemplate
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.util.*

class ResponseCollector : InsightsCollector {

    data class Data(
        val status: HttpStatusCode?,
        val headers: Map<String, List<String>>,
    ) : InsightsCollectorData {

        override fun renderDetails(template: InsightsGuiTemplate) = with(template) {

            menu {
                icon.cloud_download_alternate()
                +"Response"
            }

            content {
                json(this@Data)
            }
        }
    }

    override fun finish(call: ApplicationCall) = Data(
        call.response.status(),
        call.response.headers.allValues().toMap()
    )
}
