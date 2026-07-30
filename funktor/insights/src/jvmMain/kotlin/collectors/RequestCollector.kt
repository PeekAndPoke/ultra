package io.peekandpoke.funktor.insights.collectors

import io.ktor.http.HttpMethod
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.origin
import io.ktor.server.request.host
import io.ktor.server.request.httpMethod
import io.ktor.server.request.port
import io.ktor.server.request.uri
import io.ktor.util.toMap
import io.peekandpoke.funktor.insights.InsightsCollector
import io.peekandpoke.funktor.insights.InsightsCollectorData
import io.peekandpoke.funktor.insights.HeaderLogging

class RequestCollector(
    private val headerLogging: HeaderLogging,
) : InsightsCollector {

    /** VUE-REF: `reference/collectors/RequestCollector.kt` */
    data class Data(
        val method: HttpMethod,
        val scheme: String,
        val host: String,
        val port: Int,
        val uri: String,
        val headers: Map<String, List<String>>,
        val queryParams: Map<String, List<String>>,
    ) : InsightsCollectorData {
        override val key = KEY

        companion object {
            const val KEY = "request"
        }
    }

    override fun finish(call: ApplicationCall) = Data(
        method = call.request.httpMethod,
        scheme = call.request.origin.scheme,
        host = call.request.host(),
        port = call.request.port(),
        uri = call.request.uri,
        headers = headerLogging.applyTo(call.request.headers.toMap()),
        queryParams = call.request.queryParameters.toMap(),
    )
}
