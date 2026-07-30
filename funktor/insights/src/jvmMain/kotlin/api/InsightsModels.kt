package io.peekandpoke.funktor.insights.api

import io.peekandpoke.ultra.datetime.MpInstant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * One record in the list view.
 *
 * The summary fields are derived from the `request` and `response` slices — the same derivations the
 * deleted `InsightsGuiData` did for its header — because a list of requests without method, url and
 * status is not usable.
 */
@Serializable
data class InsightsRecordSummary(
    /** Depot path, and the id used to fetch the full record. */
    val path: String,
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
    val path: String,
    val recordedAt: MpInstant?,
    val durationMs: Double,
    val collectors: List<InsightsCollectorSlice>,
    /** Neighbouring records in time, for prev/next navigation. Null at either end. */
    val nextPath: String?,
    val previousPath: String?,
)

/**
 * One collector's slice: its declared key and its payload, untyped on purpose.
 *
 * [data] stays a [JsonElement] so the envelope never has to name every collector — a sealed hierarchy
 * here would make an app-defined collector impossible. Type safety comes back per tab: each collector's
 * `Data` class is a codegen root, so the frontend gets a generated schema for exactly its own slice.
 */
@Serializable
data class InsightsCollectorSlice(
    val key: String,
    val data: JsonElement,
)
