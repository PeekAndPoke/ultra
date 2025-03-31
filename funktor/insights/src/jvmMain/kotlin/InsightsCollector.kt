package de.peekandpoke.funktor.insights

import io.ktor.server.application.*

interface InsightsCollector {
    fun finish(call: ApplicationCall): InsightsCollectorData
}
