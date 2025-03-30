package de.peekandpoke.ktorfx.insights.collectors

import de.peekandpoke.kraft.semanticui.icon
import de.peekandpoke.ktorfx.insights.InsightsCollector
import de.peekandpoke.ktorfx.insights.InsightsCollectorData
import de.peekandpoke.ktorfx.insights.gui.InsightsGuiTemplate
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.util.*

class RequestCollector : InsightsCollector {

    data class Data(
        val method: HttpMethod,
        val scheme: String,
        val host: String,
        val port: Int,
        val uri: String,
        val headers: Map<String, List<String>>,
        val queryParams: Map<String, List<String>>,
    ) : InsightsCollectorData {

        val fullUrl = "$scheme://$host:$port$uri"

        override fun renderDetails(template: InsightsGuiTemplate) = with(template) {

            menu {
                icon.cloud_upload_alternate()
                +"Request"
            }

            content {
                json(this@Data)
            }
        }
    }

    override fun finish(call: ApplicationCall) = Data(
        call.request.httpMethod,
        call.request.origin.scheme,
        call.request.host(),
        call.request.port(),
        call.request.uri,
        call.request.headers.toMap().map { (k, v) ->
            // we prevent login the full auth token
            if (k.lowercase() == "authorization") {
                k to v.map { it.substring(0, 20) + "..." }
            } else {
                k to v
            }
        }.toMap(),
        call.request.queryParameters.toMap()
    )
}
