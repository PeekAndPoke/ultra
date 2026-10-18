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
    val ts: LocalDateTime,
    val date: String,
    val startedNs: Long,
    val endedNs: Long,
    /** Headline: the request method, e.g. `GET`. */
    val method: String? = null,
    /**
     * Headline: the request **path**, query string excluded.
     *
     * The writer stores `request.path()` deliberately — `request.uri` carries the query string, and
     * storing it verbatim put every `?token=` into the record and into the list. The parameters live
     * in the `request` slice, redacted by name.
     */
    val uri: String? = null,
    /** Headline: the response status code. */
    val status: Int? = null,
    /** Empty at [InsightsLevel.BRIEF]. */
    val collectors: List<CollectorData>,
)

/**
 * One collector's slice as stored on disk: its declared [key] plus the raw Jackson tree.
 *
 * [key] used to be the collector's JVM class name, which the loader fed to `Class.forName` — that ran
 * static initializers of whatever a stored file named, before any type check. A declared key is looked
 * up, never loaded.
 */
data class CollectorData(val key: String, val data: Map<*, *>)
