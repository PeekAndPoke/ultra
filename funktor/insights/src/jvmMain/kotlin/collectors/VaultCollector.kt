package io.peekandpoke.funktor.insights.collectors

import io.ktor.server.application.ApplicationCall
import io.peekandpoke.funktor.insights.InsightsCollector
import io.peekandpoke.funktor.insights.InsightsCollectorData
import io.peekandpoke.ultra.vault.profiling.QueryProfiler

/**
 * Database queries run while handling the request.
 *
 * ## Why there is an explicit DTO here
 *
 * This collector used to store `List<QueryProfiler.Entry.Impl>` directly, and **that could never be
 * written**: `Entry.Impl` is a plain class with a three-argument constructor, so `BuiltInModule` has no
 * codec branch for it (not `isData`, not an empty primary constructor) and `SlumberConfig` raised
 * *"no known way to slumber the type"*. That happened inside `launch(Dispatchers.IO)` after the response
 * had been sent, so the failure silently dropped the **entire record**, not merely this slice.
 *
 * Found by the `/feature-review` gate on 2026-08-24 and confirmed across 1193 real depot records:
 * `vault.entries` was `[]` in every single one, on every app that ever ran with profiling on.
 *
 * Two further traps the DTO removes, both of which would have produced a slice full of zeros even once
 * the codec existed:
 *
 * - `StopWatch.Impl.totalNs` and `.count` are **computed getters** over `entriesNs`, so a codec that
 *   emits constructor parameters writes `{}` for them.
 * - `Entry.totalNs` is an interface getter, likewise never emitted.
 *
 * Everything the frontend needs is therefore a constructor parameter of a `data class` below.
 */
class VaultCollector(private val profiler: QueryProfiler) : InsightsCollector {

    override val key = KEY

    companion object {
        /**
         * **The frontend keys its tab on this exact string.** `InsightsDetailPage.vue`'s registry maps
         * `"vault"` to the tab that withholds bind values; an unregistered key falls through to a raw
         * JSON view. So renaming this does not break a build — it silently swaps a redacting view for a
         * dumping one. Pinned by `VaultCollectorKeySpec`.
         */
        const val KEY = "vault"
    }

    /**
     * One stop watch, flattened to the two numbers a reader wants.
     *
     * A `data class` with both as constructor parameters, precisely because [QueryProfiler.StopWatch]
     * exposes them as computed getters that no codec emits.
     */
    data class Measure(
        val totalNs: Long,
        val count: Int,
    ) {
        companion object {
            fun of(watch: QueryProfiler.StopWatch) = Measure(totalNs = watch.totalNs, count = watch.count)
        }
    }

    /**
     * One profiled query.
     *
     * **[query] never contains bind values, and that is an invariant of this type, not a hope.** A driver
     * that inlines its values into the query text must not set it — see [Data.of]. The values live in
     * [varsCount] only, as a count: the values themselves are deliberately not recorded here, because a
     * query is how a session token or an activation code is looked up.
     */
    data class Entry(
        val connection: String?,
        val queryLanguage: String?,
        /** Placeholder-ised query text, or null when the driver could not provide one safely. */
        val query: String?,
        /** How many bind variables the query had. The VALUES are not recorded. */
        val varsCount: Int,
        val count: Long?,
        val totalCount: Long?,
        val totalNs: Long,
        val measureQuery: Measure,
        val measureIterator: Measure,
        val measureSerializer: Measure,
        val measureDeserializer: Measure,
        val measureExplain: Measure,
    )

    data class Data(
        val entries: List<Entry>,
    ) : InsightsCollectorData {
        companion object {
            /**
             * Query languages whose recorded text is known to keep bind values OUT of it.
             *
             * Kept HERE rather than in the frontend, and it is the last place it should ever live: this
             * is where a driver's output is turned into a record, so it is the one point that can
             * enforce the invariant for every consumer at once. The frontend renders [Entry.query]
             * unconditionally and needs no such list.
             *
             * `aql` qualifies: Karango records the query with its `@placeholders` intact and passes
             * values separately. Mongo does not — `MonkoDriver` records the filter document *with* its
             * values and leaves `vars` empty — so its text is dropped until that is fixed at the driver.
             */
            private val PLACEHOLDER_SAFE_LANGUAGES = setOf("aql")

            /**
             * Takes [QueryProfiler.Entry.Impl], not the `Entry` interface.
             *
             * `connection` and `queryLanguage` are declared on the implementation only, so the interface
             * cannot supply them — and widening `ultra:vault`'s public interface to suit one consumer is
             * the wrong direction. The previous version filtered the same way; this keeps that, which
             * also excludes `Entry.Null`.
             */
            fun of(entries: List<QueryProfiler.Entry.Impl>) = Data(
                entries = entries.map { entry ->
                    Entry(
                        connection = entry.connection,
                        queryLanguage = entry.queryLanguage,
                        query = entry.query.takeIf { entry.queryLanguage in PLACEHOLDER_SAFE_LANGUAGES },
                        varsCount = entry.vars?.size ?: 0,
                        count = entry.count,
                        totalCount = entry.totalCount,
                        totalNs = entry.totalNs,
                        measureQuery = Measure.of(entry.measureQuery),
                        measureIterator = Measure.of(entry.measureIterator),
                        measureSerializer = Measure.of(entry.measureSerializer),
                        measureDeserializer = Measure.of(entry.measureDeserializer),
                        measureExplain = Measure.of(entry.measureExplain),
                    )
                },
            )
        }
    }

    /**
     * **`queryExplained` is deliberately absent from the DTO.**
     *
     * It cannot be made safe by placeholder-ising, because the DATABASE substitutes the values into the
     * plan it returns — measured against a real Arango server, which answered with
     * `FILTER ((d.token == "<the actual token>"))`. Mongo's `QUERY_PLANNER` output carries the same
     * literals in `parsedQuery`. Recording it would put a live session token in a depot file.
     */
    override fun finish(call: ApplicationCall): Data =
        Data.of(profiler.entries.filterIsInstance<QueryProfiler.Entry.Impl>())
}
