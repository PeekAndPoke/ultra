package io.peekandpoke.funktor.insights.collectors

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.util.*
import io.peekandpoke.funktor.insights.InsightsCollector
import io.peekandpoke.funktor.insights.InsightsCollectorData
import io.peekandpoke.funktor.insights.gui.InsightsGuiTemplate
import io.peekandpoke.ultra.semanticui.icon

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
