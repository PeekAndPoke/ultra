package impl

import de.peekandpoke.funktor.core.model.InsightsConfig
import de.peekandpoke.funktor.insights.Insights
import io.ktor.server.application.*

class InsightsSlim(override val config: InsightsConfig) : Insights.Base() {
    override fun getRequestDetailsUri(): String? = null

    override fun getRequestDetailsUrl(): String? = null

    override suspend fun finish(call: ApplicationCall) {
        // noop
    }
}
