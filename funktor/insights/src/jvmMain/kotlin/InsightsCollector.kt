package de.peekandpoke.ktorfx.insights

import io.ktor.server.application.*

interface InsightsCollector {
    fun finish(call: ApplicationCall): InsightsCollectorData
}
