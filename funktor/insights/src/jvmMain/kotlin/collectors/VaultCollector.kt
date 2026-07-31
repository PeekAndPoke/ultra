package io.peekandpoke.funktor.insights.collectors

import io.ktor.server.application.*
import io.peekandpoke.funktor.insights.InsightsCollector
import io.peekandpoke.funktor.insights.InsightsCollectorData
import io.peekandpoke.ultra.vault.profiling.QueryProfiler

class VaultCollector(private val profiler: QueryProfiler) : InsightsCollector {

    override val key = KEY

    companion object {
        const val KEY = "vault"
    }

    /** VUE-REF: `reference/collectors/VaultCollector.kt` */
    data class Data(
        val entries: List<QueryProfiler.Entry.Impl>,
    ) : InsightsCollectorData {
    }


    override fun finish(call: ApplicationCall): Data {
        val entries = profiler.entries.filterIsInstance<QueryProfiler.Entry.Impl>()

        return Data(entries = entries)
    }
}
