package impl

import io.ktor.server.application.*
import io.peekandpoke.funktor.core.model.InsightsConfig
import io.peekandpoke.funktor.insights.Insights
import io.peekandpoke.funktor.rest.InsightsLevel
import io.peekandpoke.funktor.rest.InsightsOptions

/**
 * Records nothing at all — the implementation used when insights is disabled app-wide.
 *
 * Named `InsightsSlim` until 2026-07-31, which read as "records less" while it actually records none.
 * Per-request granularity is [InsightsLevel] on the route, a separate axis from this one.
 */
class InsightsDisabled(override val config: InsightsConfig) : Insights.Base() {
    override suspend fun finish(call: ApplicationCall, options: InsightsOptions) {
        // noop
    }
}
