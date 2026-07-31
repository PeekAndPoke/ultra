package io.peekandpoke.funktor.insights

import io.ktor.server.application.ApplicationCall

/** Collects diagnostic data during a request, producing [InsightsCollectorData] on finish. */
interface InsightsCollector {
    /**
     * Stable identifier for this collector's slice of a record, e.g. `"request"`.
     *
     * **Declared, never derived from a class name.** The frontend addresses tabs by this string, so
     * deriving it — as `templateKey` used to, from the qualified class name — would silently orphan a
     * tab on any rename or package move.
     *
     * It lives on the COLLECTOR rather than on its `Data`, because a value on the data class is a
     * getter that Jackson serialises into the payload as well as onto the envelope. That is the same
     * shape-skew `fullUrl` was removed for: Jackson emits it, Slumber (constructor parameters only)
     * would not, so a generated per-tab schema would not describe a field present in every record.
     */
    val key: String

    fun finish(call: ApplicationCall): InsightsCollectorData
}
