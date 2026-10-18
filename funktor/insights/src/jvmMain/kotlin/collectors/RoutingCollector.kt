package io.peekandpoke.funktor.insights.collectors

import io.ktor.server.application.ApplicationCall
import io.peekandpoke.funktor.insights.InsightsCollector
import io.peekandpoke.funktor.insights.InsightsCollectorData
import io.peekandpoke.funktor.insights.RoutingInstrumentation

class RoutingCollector : InsightsCollector {

    override val key = KEY

    companion object {
        const val KEY = "routing"
    }

    /** VUE-REF: `reference/collectors/RoutingCollector.kt` */
    data class Data(
        val trace: String? = null,
    ) : InsightsCollectorData

    private var data: Data = Data()

    override fun finish(call: ApplicationCall): InsightsCollectorData {
        val trace = call.attributes.getOrNull(RoutingInstrumentation.Key)

        return data.copy(
            trace = trace?.buildText()
        )
    }
}
