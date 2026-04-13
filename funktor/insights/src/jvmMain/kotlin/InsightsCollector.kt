package io.peekandpoke.funktor.insights

import io.ktor.server.application.*

/** Collects diagnostic data during a request, producing [InsightsCollectorData] on finish. */
interface InsightsCollector {
    fun finish(call: ApplicationCall): InsightsCollectorData
}
