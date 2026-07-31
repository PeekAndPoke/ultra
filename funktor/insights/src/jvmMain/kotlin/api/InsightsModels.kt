package io.peekandpoke.funktor.insights.api

import io.peekandpoke.ultra.datetime.MpInstant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Addresses one record, in exactly the shape the detail endpoint takes.
 *
 * Deliberately NOT a single `"records-<date>/<file>.json"` string. The endpoint is
 * `/records/{bucket}/{file}`, so a flat path would force every generated call site to do
 * `path.split('/')` — re-implementing the depot's layout in the frontend, and silently wrong the day a
 * repository nests differently or a name contains a slash.
 */
@Serializable
data class InsightsRecordRef(
    val bucket: String,
    val file: String,
) {
    /** The depot path this addresses. A function, not a property — Slumber emits constructor params only. */
    fun toPath(): String = "$bucket/$file"
}

/**
 * One record in the list view.
 *
 * The headline is read straight off the stored record — see `InsightsData` for why it is stored rather
 * than derived from the request/response slices.
 */
@Serializable
data class InsightsRecordSummary(
    /** Pass straight to the detail endpoint; no string splitting required. */
    val ref: InsightsRecordRef,
    /**
     * When the record was written.
     *
     * Taken from the depot file rather than the record's own `ts`, which is a `java.time.LocalDateTime`:
     * that needs a `JavaTimeTsContributor` for codegen, which does not exist, whereas `MpInstant` is
     * already claimed by `MpDateTimeTsContributor`.
     */
    val recordedAt: MpInstant?,
    val method: String?,
    val url: String?,
    val status: Int?,
    val durationMs: Double?,
)

/**
 * One full insights record.
 *
 * [collectors] is an **open envelope**: the framework does not enumerate what may appear, so an
 * application-defined collector is indistinguishable from a built-in one. A frontend tab looks up its
 * own slice by key and parses it against its own schema.
 */
@Serializable
data class InsightsRecord(
    val ref: InsightsRecordRef,
    val recordedAt: MpInstant?,
    /** Null when the record carries no timing — distinct from a genuine 0.0. */
    val durationMs: Double?,
    val collectors: List<InsightsCollectorSlice>,
    /**
     * Neighbouring records **within the same day folder**, for prev/next navigation.
     *
     * Null at the edges of that folder — which is not the same as the edges of the depot: the oldest
     * record of a day reports no previous even when yesterday's records exist. Crossing day folders is
     * not implemented.
     */
    val next: InsightsRecordRef?,
    val previous: InsightsRecordRef?,
)

/**
 * One collector's slice: its declared key and its payload, untyped on purpose.
 *
 * [data] stays a [JsonElement] so the envelope never has to name every collector — a sealed hierarchy
 * here would make an app-defined collector impossible. Type safety comes back per tab: each collector's
 * `Data` class is a codegen root, so the frontend gets a generated schema for exactly its own slice.
 *
 * **Every string inside [data] is attacker-controlled.** Headers, query parameters, paths and user
 * agents are all recorded verbatim from unauthenticated requests, so a payload may contain
 * `<img src=x onerror=…>` or anything else. The kotlinx.html GUI this replaced escaped on output; a
 * JSON API cannot, so escaping is now the consumer's obligation. **Render with Vue's normal text
 * interpolation — `{{ }}` or `v-text`, both of which escape — and NEVER with `v-html`.**
 */
@Serializable
data class InsightsCollectorSlice(
    val key: String,
    val data: JsonElement,
)
