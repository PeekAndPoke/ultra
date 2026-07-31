package io.peekandpoke.funktor.insights.collectors

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.responseType
import io.ktor.util.toMap
import io.peekandpoke.funktor.insights.InsightsCollector
import io.peekandpoke.funktor.insights.InsightsCollectorData
import io.peekandpoke.funktor.insights.HeaderLogging

class ResponseCollector(
    private val headerLogging: HeaderLogging,
) : InsightsCollector {

    override val key = KEY

    companion object {
        const val KEY = "response"
    }

    /** VUE-REF: `reference/collectors/ResponseCollector.kt` */
    data class Data(
        val status: HttpStatusCode?,
        val headers: Map<String, List<String>>,
    ) : InsightsCollectorData {
    }

    override fun finish(call: ApplicationCall) = Data(
        status = call.response.status(),
        // Set-Cookie lives here: an unredacted login response record contains a working session.
        headers = headerLogging.applyTo(call.response.headers.allValues().toMap()),
    )
}
