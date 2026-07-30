package io.peekandpoke.funktor.insights.collectors

import io.ktor.server.application.ApplicationCall
import io.peekandpoke.funktor.insights.InsightsCollector
import io.peekandpoke.funktor.insights.InsightsCollectorData

class TemplateInsightsCollector : InsightsCollector {

    /** VUE-REF: `reference/collectors/TemplateInsightsCollector.kt` */
    data class Data(
        val timeNs: Long? = null,
    ) : InsightsCollectorData {
        override val key = KEY

        companion object {
            const val KEY = "template"
        }
    }

    var data = Data()

    override fun finish(call: ApplicationCall): InsightsCollectorData = data

    fun record(timeNs: Long) {
        data = Data(timeNs)
    }
}
