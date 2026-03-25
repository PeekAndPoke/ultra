package impl

import io.ktor.server.application.*
import io.peekandpoke.funktor.core.model.InsightsConfig
import io.peekandpoke.funktor.insights.Insights

class InsightsSlim(override val config: InsightsConfig) : Insights.Base() {
    override fun getRequestDetailsUri(): String? = null

    override fun getRequestDetailsUrl(): String? = null

    override suspend fun finish(call: ApplicationCall) {
        // noop
    }
}
