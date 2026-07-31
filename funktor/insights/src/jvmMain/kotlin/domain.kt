package io.peekandpoke.funktor.insights

import io.peekandpoke.funktor.rest.InsightsLevel
import java.time.LocalDateTime

/**
 * One insights record as stored on disk.
 *
 * The **headline** — [method], [uri], [status] — is stored rather than derived. It used to be dug out
 * of the `request` and `response` collector slices, which meant listing N records had to parse all of
 * them: the kontainer slice alone averages 137 KB, so a 50-row list read 8.3 MB of JSON for five
 * scalars. Storing it also makes [InsightsLevel.BRIEF] possible at all — a brief record has no
 * collectors for a headline to be derived from.
 */
data class InsightsData(
    /**
     * Version of this stored shape.
     *
     * Legacy records have no `formatVersion` at all and use a different collector-entry shape. Without
     * this the loader would have to guess a record's format from which fields happen to be present.
     */
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    val ts: LocalDateTime,
    val date: String,
    val startedNs: Long,
    val endedNs: Long,
    /** Headline: the request method, e.g. `GET`. */
    val method: String? = null,
    /** Headline: the request uri, query string included. */
    val uri: String? = null,
    /** Headline: the response status code. */
    val status: Int? = null,
    /** Empty at [InsightsLevel.BRIEF]. */
    val collectors: List<CollectorData>,
) {
    companion object {
        /**
         * Bump when the stored shape changes in a way a reader must branch on.
         *
         * 1 — headline fields promoted to the top level; collector entries keyed by the collector's
         * declared `key`. Records with no `formatVersion` are pre-1 and key their entries by JVM class
         * name under `cls`.
         */
        const val CURRENT_FORMAT_VERSION = 1
    }
}

/**
 * One collector's slice as stored on disk: its declared [key] plus the raw Jackson tree.
 *
 * [key] used to be the collector's JVM class name, which the loader fed to `Class.forName` — that ran
 * static initializers of whatever a stored file named, before any type check. A declared key is looked
 * up, never loaded.
 */
data class CollectorData(val key: String, val data: Map<*, *>)
