package impl

import de.peekandpoke.ktorfx.core.model.InsightsConfig
import de.peekandpoke.ktorfx.insights.Insights
import io.ktor.server.application.*

class InsightsSlim(override val config: InsightsConfig) : Insights.Base() {
    override fun getRequestDetailsUri(): String? = null

    override suspend fun finish(call: ApplicationCall) {
        // noop
    }
}
